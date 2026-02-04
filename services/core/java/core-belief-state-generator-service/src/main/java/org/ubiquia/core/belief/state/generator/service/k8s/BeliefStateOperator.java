package org.ubiquia.core.belief.state.generator.service.k8s;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
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
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.core.belief.state.generator.service.builder.BeliefStateDeploymentBuilder;

@ConditionalOnProperty(
    value = "ubiquia.kubernetes.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class BeliefStateOperator {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateOperator.class);
    private final Integer maxDeploymentRetries = 10;
    private V1Deployment ubiquiaDeployment;
    private ApiClient apiClient;
    @Autowired
    private BeliefStateDeploymentBuilder beliefStateDeploymentBuilder;
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
        this.beliefStateDeploymentBuilder.setUbiquiaDeployment(this.ubiquiaDeployment);
        this.initializeDeploymentUpdateInformer();
        logger.info("...Kubernetes client connection initialized.");
    }

    @PreDestroy
    public void teardown() throws JsonProcessingException {
        logger.info("Tearing down Ubiquia belief state operator...");

        var deploymentResponse = this.deploymentClient.get(this.namespace, "ubiquia-core-belief-state-generator-service");
        if (deploymentResponse.getHttpStatusCode() == HttpStatus.NO_CONTENT.value()) {
            logger.info("...Ubiquia K8s deployment not found, tearing down "
                + "deployed belief states...");
            this.tryDeleteAllDeployedBeliefStateResources();
        } else if (!deploymentResponse.isSuccess()) {
            logger.info("...unsuccessful request for Ubiquia K8s deployment; assuming to "
                + "tear down deployed belief states...");
            this.tryDeleteAllDeployedBeliefStateResources();
        }
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

    public void tryDeployBeliefState(final DomainOntology domainOntology)
        throws JsonProcessingException {

        logger.info("Trying to deploy a belief state for domain: \nName: {} \nVersion: {}",
            domainOntology.getName(),
            domainOntology.getVersion());

        var currentDeployment = this.deploymentClient.get(
            this.namespace,
            domainOntology.getName().toLowerCase() + domainOntology.getVersion());

        if (Objects.isNull(currentDeployment.getObject())) {
            logger.info("...no current deployment exists, attempting to deploy...");

            var deployment = this
                .beliefStateDeploymentBuilder
                .buildDeploymentFrom(domainOntology);

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

            var service = this.beliefStateDeploymentBuilder.buildServiceFrom(domainOntology);
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

            logger.info("...completed deployment...");
        } else {
            logger.info("...deployment found for that domain; not deploying...");
        }
    }

    public void deleteBeliefStateResources(final String beliefStateName) {
        logger.info("...deleting K8s resources for belief state: {}...", beliefStateName);

        var listOptions = new ListOptions();
        listOptions.setLabelSelector("belief-state=" + beliefStateName);

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

        logger.info("...deleted.");
    }

    /**
     * Delete any deployed DAG resources.
     *
     * @param deployment The deployment containing the deleted Ubiquia resource.
     */
    private void tryProcessDeploymentDeletion(V1Deployment deployment) {
        if (deployment.getMetadata().getName().equals("ubiquia-core-belief-state-generator-service")) {
            logger.info("Received a deletion update for Ubiquia itself; proceeding to teardown"
                + " any deployed belief state resources...");
            try {
                this.tryDeleteAllDeployedBeliefStateResources();
            } catch (Exception e) {
                logger.error("ERROR: Could not delete belief states: {}", e.getMessage());
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
                var response = this.deploymentClient.get(this.namespace, "ubiquia-core-belief-state-generator-service");
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
    public void tryDeleteAllDeployedBeliefStateResources() throws JsonProcessingException {
        var listOptions = new ListOptions();

        listOptions.setLabelSelector("belief-state");
        var response = this.deploymentClient.list(this.namespace, listOptions);

        if (response.isSuccess()) {
            for (var deployedBeliefState : response.getObject().getItems()) {
                logger.info("...deleting belief state resources with name: {}...",
                    deployedBeliefState.getMetadata().getName());
                this.deploymentClient.delete(
                    this.namespace,
                    deployedBeliefState.getMetadata().getName());
                this.serviceClient.delete(
                    this.namespace,
                    deployedBeliefState.getMetadata().getName());
            }
        } else {
            throw new IllegalArgumentException("ERROR: Got response from Kubernetes: "
                + this.objectMapper.writeValueAsString(response));
        }
    }
}
