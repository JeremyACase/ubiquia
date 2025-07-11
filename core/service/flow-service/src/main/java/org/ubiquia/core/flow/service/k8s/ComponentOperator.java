package org.ubiquia.core.flow.service.k8s;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.util.generic.options.CreateOptions;
import io.kubernetes.client.util.generic.options.ListOptions;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Component;
import org.ubiquia.core.flow.service.builder.ComponentDeploymentBuilder;

/**
 * This is a service that will deploy and tear down componnets by communicating with the
 * Kubernetes API server.
 */
@ConditionalOnProperty(
    value = "ubiquia.kubernetes.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class ComponentOperator {

    private static final Logger logger = LoggerFactory.getLogger(ComponentOperator.class);
    private final Integer maxDeploymentRetries = 10;
    private V1Deployment ubiquiaDeployment;
    private ApiClient apiClient;
    private GenericKubernetesApi<V1ConfigMap, V1ConfigMapList> configMapClient;
    @Autowired
    private ComponentDeploymentBuilder componentDeploymentBuilder;
    private GenericKubernetesApi<V1Deployment, V1DeploymentList> deploymentClient;
    private Integer deploymentRetries = 0;
    @Value("${ubiquia.kubernetes.namespace}")
    private String namespace;
    @Autowired
    private ObjectMapper objectMapper;
    private GenericKubernetesApi<V1Service, V1ServiceList> serviceClient;

    /**
     * Initialize this operator with connections to the Kubernetes API server.
     *
     * @throws IOException Exceptions from not being able to connect to Kubernetes.
     */
    public void init() throws IOException, InterruptedException {
        logger.info("Initializing Kubernetes client connection...");
        this.apiClient = ClientBuilder.standard().build();
        this.configMapClient = new GenericKubernetesApi<>(
            V1ConfigMap.class,
            V1ConfigMapList.class,
            "",
            "v1",
            "configmaps",
            apiClient);
        this.deploymentClient = new GenericKubernetesApi<>(
            V1Deployment.class,
            V1DeploymentList.class,
            "apps",
            "v1",
            "deployments",
            apiClient);
        this.serviceClient = new GenericKubernetesApi<>(
            V1Service.class,
            V1ServiceList.class,
            "",
            "v1",
            "services",
            apiClient);
        this.tryCacheUbiquiaDeploymentFromKubernetes();
        this.componentDeploymentBuilder.setUbiquiaDeployment(this.ubiquiaDeployment);
        this.initializeDeploymentUpdateInformer();
        logger.info("...Kubernetes client connection initialized.");
    }

    @PreDestroy
    public void teardown() throws JsonProcessingException {
        logger.info("Tearing down Ubiquia agent operator...");

        var deploymentResponse = this.deploymentClient.get(this.namespace, "ubiquia-core-flow-service");
        if (deploymentResponse.getHttpStatusCode() == HttpStatus.NO_CONTENT.value()) {
            logger.info("...Ubiquia K8s deployment not found, tearing down "
                + "deployed graphs...");
            this.tryDeleteAllDeployedGraphResources();
        } else if (!deploymentResponse.isSuccess()) {
            logger.info("...unsuccessful request for Ubiquia K8s deployment; assuming to "
                + "tear down deployed graphs...");
            this.tryDeleteAllDeployedGraphResources();
        }
    }

    /**
     * Delete a graph's deployed resources from Kubernetes.
     *
     * @param graphName The graph to delete.
     */
    public void deleteGraphResources(final String graphName) {
        logger.info("...deleting K8s resources for graph: {}...", graphName);

        var listOptions = new ListOptions();
        listOptions.setLabelSelector("ubiquia-graph=" + graphName);

        var deployments = this.deploymentClient.list(this.namespace, listOptions);
        for (var deployment : deployments.getObject().getItems()) {
            logger.info("...deleting deployment with name: {}...",
                deployment.getMetadata().getName());
            this.deploymentClient.delete(
                this.namespace,
                deployment.getMetadata().getName());
        }

        var services = this.serviceClient.list(this.namespace, listOptions);
        for (var service : services.getObject().getItems()) {
            logger.info("...deleting service with name: {}...",
                service.getMetadata().getName());
            this.deploymentClient.delete(
                this.namespace,
                service.getMetadata().getName());
        }

        var configMaps = this.configMapClient.list(this.namespace, listOptions);
        for (var configMap : configMaps.getObject().getItems()) {
            logger.info("...deleting config map with name: {}...",
                configMap.getMetadata().getName());
            this.deploymentClient.delete(
                this.namespace,
                configMap.getMetadata().getName());
        }

        logger.info("...deleted.");
    }

    /**
     * Initialize our Kubernetes Informer for deployments so we can listen to when
     * updates are applied to Ubiquia itself, so that we can in turn update the deployed
     * DAG's accordingly.
     */
    private void initializeDeploymentUpdateInformer() {
        var factory = new SharedInformerFactory(this.apiClient);
        var informer = factory.sharedIndexInformerFor(
            this.deploymentClient,
            V1Deployment.class,
            60 * 1000L,
            this.namespace);

        // Receive updates from Kubernetes when Deployments change
        informer.addEventHandler(new ResourceEventHandler<>() {
            @Override
            public void onAdd(V1Deployment v1Deployment) {

            }

            @Override
            public void onUpdate(V1Deployment v1Deployment, V1Deployment apiType1) {

            }

            @Override
            public void onDelete(V1Deployment deployment, boolean b) {
                tryProcessDeploymentDeletion(deployment);
            }
        });

        factory.startAllRegisteredInformers();
    }

    public void tryDeployComponent(final Component component)
        throws JsonProcessingException {

        logger.info("Trying to deploy an component for with name {} for graph {}...",
            component.getName(),
            component.getGraph().getName());

        var currentDeployment = this.deploymentClient.get(
            this.namespace,
            component.getName().toLowerCase());

        if (Objects.isNull(currentDeployment.getObject())) {
            logger.info("...no current deployment exists, attempting to deploy...");

            var deployment = this
                .componentDeploymentBuilder
                .buildDeploymentFrom(component);
            logger.debug("...deploying deployment: {}...",
                this.objectMapper.writeValueAsString(deployment));
            var deploymentResponse = this.deploymentClient.create(
                this.namespace,
                deployment,
                new CreateOptions());

            if (!deploymentResponse.isSuccess()) {
                throw new IllegalArgumentException("ERROR - unable to create deployment;"
                    + " response from Kubernetes: "
                    + this.objectMapper.writeValueAsString(deploymentResponse));
            } else {
                logger.info("...deployed successfully; response code: {}",
                    deploymentResponse.getHttpStatusCode());
            }

            var service = this.componentDeploymentBuilder.buildServiceFrom(component);
            logger.debug("...deploying service: {}...",
                this.objectMapper.writeValueAsString(service));
            var serviceResponse = this.serviceClient.create(
                this.namespace,
                service,
                new CreateOptions());
            if (!serviceResponse.isSuccess()) {
                throw new IllegalArgumentException("ERROR - unable to create service; "
                    + "response from Kubernetes: "
                    + this.objectMapper.writeValueAsString(serviceResponse));
            } else {
                logger.info("...deployed successfully; response code: {}",
                    serviceResponse.getHttpStatusCode());
            }

            var configMap = this
                .componentDeploymentBuilder
                .tryBuildConfigMapFrom(component);
            if (Objects.nonNull(configMap)) {
                logger.info("...found a configmap for component; attempting to deploy...");
                var configMapResponse = this.configMapClient.create(
                    this.namespace,
                    configMap,
                    new CreateOptions());
                if (!configMapResponse.isSuccess()) {
                    throw new IllegalArgumentException("ERROR - unable to create configMap; "
                        + "response from Kubernetes: "
                        + this.objectMapper.writeValueAsString(configMapResponse));
                } else {
                    logger.info("...deployed successfully; response code: {}",
                        configMapResponse.getHttpStatusCode());
                }
            }

            logger.info("...completed deployment...");
        } else {
            logger.info("...deployment found for that component and graph; not deploying...");
        }
    }

    /**
     * Delete any deployed DAG resources.
     *
     * @param deployment The deployment containing the deleted Ubiquia resource.
     */
    private void tryProcessDeploymentDeletion(V1Deployment deployment) {
        if (deployment.getMetadata().getName().equals("ubiquia-core-flow-service")) {
            logger.info("Received a deletion update for Ubiquia itself; proceeding to teardown"
                + " any deployed DAG resources...");
            try {
                this.tryDeleteAllDeployedGraphResources();
            } catch (Exception e) {
                logger.error("ERROR: Could not delete graphs: {}", e.getMessage());
            }
        }
    }

    /**
     * Attempt to fetch the Ubiquia deployment metadata from Kubernetes for use in this service.
     *
     * @throws InterruptedException Exception from thread sleeping.
     * @throws IOException          Exception from being unable to fetch the Ubiquia deployment.
     */
    private void tryCacheUbiquiaDeploymentFromKubernetes()
        throws InterruptedException,
        IOException {
        if (this.deploymentRetries >= this.maxDeploymentRetries) {
            throw new IOException("ERROR: Unable to fetch the Ubiquia deployment even after "
                + this.maxDeploymentRetries
                + " retries...");
        } else {
            if (Objects.isNull(this.ubiquiaDeployment)) {
                logger.info("...trying to fetch Ubiquia deployment data from Kubernetes...");
                var response = this.deploymentClient.get(this.namespace, "ubiquia-core-flow-service");
                if (response.isSuccess()) {
                    this.ubiquiaDeployment = response.getObject();
                    logger.info("...successfully fetched Ubiquia deployment data from Kubernetes...");
                } else {
                    logger.error("Failed to fetch deployment: " + response.getStatus());
                    this.deploymentRetries++;
                    logger.info("...waiting and then attempting again...");
                    Thread.sleep(1000);
                    this.tryCacheUbiquiaDeploymentFromKubernetes();
                }
            }
        }
    }

    /**
     * Delete any deployed graph deployments.
     *
     * @throws JsonProcessingException Json exception from logging the K8s response.
     */
    public void tryDeleteAllDeployedGraphResources() throws JsonProcessingException {
        var listOptions = new ListOptions();

        listOptions.setLabelSelector("ubiquia-graph");
        var response = this.deploymentClient.list(this.namespace, listOptions);

        if (response.isSuccess()) {
            for (var deployedGraph : response.getObject().getItems()) {
                logger.info("...deleting graph resources with name: {}...",
                    deployedGraph.getMetadata().getName());
                this.deploymentClient.delete(
                    this.namespace,
                    deployedGraph.getMetadata().getName());
                this.serviceClient.delete(
                    this.namespace,
                    deployedGraph.getMetadata().getName());
                this.configMapClient.delete(
                    this.namespace,
                    deployedGraph.getMetadata().getName() + "-config");
            }
        } else {
            throw new IllegalArgumentException("ERROR: Got response from Kubernetes: "
                + this.objectMapper.writeValueAsString(response));
        }
    }
}
