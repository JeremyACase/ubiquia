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
import org.ubiquia.common.library.implementation.service.builder.BeliefStateNameBuilder;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.core.belief.state.generator.service.builder.BeliefStateDeploymentBuilder;

/**
 * Manages the lifecycle of belief-state Kubernetes resources (Deployments and Services).
 *
 * <p>On startup, caches the Ubiquia deployment metadata for use as a template. On teardown,
 * deletes all deployed belief-state resources if the Ubiquia deployment itself is gone.
 */
@ConditionalOnProperty(
    value = "ubiquia.kubernetes.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class BeliefStateOperator {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateOperator.class);

    private static final String UBIQUIA_DEPLOYMENT_NAME =
        "ubiquia-core-belief-state-generator-service";

    private final Integer maxDeploymentRetries = 10;

    @Value("${ubiquia.kubernetes.namespace}")
    private String namespace;

    @Autowired
    private BeliefStateDeploymentBuilder beliefStateDeploymentBuilder;

    @Autowired
    private BeliefStateNameBuilder beliefStateNameBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    private ApiClient apiClient;
    private K8sResourceClient<V1Deployment, V1DeploymentList> deploymentClient;
    private K8sResourceClient<V1Service, V1ServiceList> serviceClient;
    private V1Deployment ubiquiaDeployment;
    private Integer deploymentRetries = 0;

    /**
     * Initializes connections to the Kubernetes API server and caches the Ubiquia deployment.
     *
     * @throws IOException          if the Kubernetes client cannot be built
     * @throws InterruptedException if the retry sleep is interrupted
     */
    public void init() throws IOException, InterruptedException {
        logger.info("Initializing Kubernetes client connection...");

        this.apiClient = ClientBuilder.standard().build();

        this.deploymentClient = new K8sResourceClient<>(
            new GenericKubernetesApi<>(
                V1Deployment.class, V1DeploymentList.class,
                "apps", "v1", "deployments", this.apiClient),
            this.namespace,
            "deployment");

        this.serviceClient = new K8sResourceClient<>(
            new GenericKubernetesApi<>(
                V1Service.class, V1ServiceList.class,
                "", "v1", "services", this.apiClient),
            this.namespace,
            "service");

        this.tryCacheUbiquiaDeploymentFromKubernetes();
        this.beliefStateDeploymentBuilder.setUbiquiaDeployment(this.ubiquiaDeployment);
        this.initializeDeploymentUpdateInformer();

        logger.info("...Kubernetes client connection initialized.");
    }

    /**
     * Tears down deployed belief states when Ubiquia itself is removed from the cluster.
     *
     * @throws JsonProcessingException if a Kubernetes response cannot be serialized for logging
     */
    @PreDestroy
    public void teardown() throws JsonProcessingException {
        logger.info("Tearing down Ubiquia belief state operator...");

        var deploymentResponse = this.deploymentClient.get(UBIQUIA_DEPLOYMENT_NAME);
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
     * Creates a Kubernetes Deployment and Service for the given domain ontology if they do not
     * already exist.
     *
     * @param domainOntology the domain ontology defining the belief state
     * @throws JsonProcessingException if a manifest cannot be serialized for logging
     */
    public void tryDeployBeliefState(final DomainOntology domainOntology)
        throws JsonProcessingException {

        logger.info("Trying to deploy a belief state for domain: \nName: {} \nVersion: {}",
            domainOntology.getName(),
            domainOntology.getVersion());

        var beliefStateName =
            this.beliefStateNameBuilder.getKubernetesBeliefStateNameFrom(domainOntology);
        var currentDeployment = this.deploymentClient.get(beliefStateName);

        if (Objects.isNull(currentDeployment.getObject())) {
            logger.info("...no current deployment exists, attempting to deploy...");

            var deployment = this.beliefStateDeploymentBuilder.buildDeploymentFrom(domainOntology);
            logger.debug("...deploying deployment: {}...",
                this.objectMapper.writeValueAsString(deployment));

            var deploymentResponse = this.deploymentClient.create(deployment, new CreateOptions());
            if (!deploymentResponse.isSuccess()) {
                throw new IllegalArgumentException("ERROR - unable to create deployment;"
                    + " response from Kubernetes: "
                    + this.objectMapper.writeValueAsString(deploymentResponse));
            }
            logger.info("...deployed successfully; response code: {}",
                deploymentResponse.getHttpStatusCode());

            var service = this.beliefStateDeploymentBuilder.buildServiceFrom(domainOntology);
            logger.debug("...deploying service: {}...",
                this.objectMapper.writeValueAsString(service));

            var serviceResponse = this.serviceClient.create(service, new CreateOptions());
            if (!serviceResponse.isSuccess()) {
                throw new IllegalArgumentException("ERROR - unable to create service; "
                    + "response from Kubernetes: "
                    + this.objectMapper.writeValueAsString(serviceResponse));
            }
            logger.info("...deployed successfully; response code: {}",
                serviceResponse.getHttpStatusCode());

            logger.info("...completed deployment...");
        } else {
            logger.info("...deployment found for that domain; not deploying...");
        }
    }

    /**
     * Deletes the Deployment and Service associated with the given belief-state name.
     *
     * @param beliefStateName the belief-state label value used to select resources
     */
    public void deleteBeliefStateResources(final String beliefStateName) {
        logger.info("...deleting K8s resources for belief state: {}...", beliefStateName);
        var selector = "belief-state=" + beliefStateName;
        this.deploymentClient.deleteBySelector(selector);
        this.serviceClient.deleteBySelector(selector);
        logger.info("...deleted.");
    }

    /**
     * Deletes all Deployments and their corresponding Services that carry a {@code belief-state}
     * label, regardless of value.
     *
     * @throws JsonProcessingException if the Kubernetes list response cannot be serialized
     */
    public void tryDeleteAllDeployedBeliefStateResources() throws JsonProcessingException {
        var listOptions = new ListOptions();
        listOptions.setLabelSelector("belief-state");
        var response = this.deploymentClient.list(listOptions);

        if (response.isSuccess()) {
            for (var item : response.getObject().getItems()) {
                var name = item.getMetadata().getName();
                logger.info("...deleting belief state resources with name: {}...", name);
                this.deploymentClient.delete(name);
                this.serviceClient.delete(name);
            }
        } else {
            throw new IllegalArgumentException("ERROR: Got response from Kubernetes: "
                + this.objectMapper.writeValueAsString(response));
        }
    }

    private void initializeDeploymentUpdateInformer() {
        var factory = new SharedInformerFactory(this.apiClient);
        var informer = factory.sharedIndexInformerFor(
            this.deploymentClient.getApi(),
            V1Deployment.class,
            60 * 1000L,
            this.namespace);

        informer.addEventHandler(new ResourceEventHandler<>() {
            @Override
            public void onAdd(final V1Deployment v1Deployment) {
            }

            @Override
            public void onUpdate(final V1Deployment v1Deployment, final V1Deployment apiType1) {
            }

            @Override
            public void onDelete(final V1Deployment deployment, final boolean b) {
                tryProcessDeploymentDeletion(deployment);
            }
        });

        factory.startAllRegisteredInformers();
    }

    /**
     * Handles a Kubernetes deployment-deletion event by tearing down belief states if the
     * deleted deployment is Ubiquia itself.
     *
     * @param deployment the deployment that was deleted
     */
    private void tryProcessDeploymentDeletion(final V1Deployment deployment) {
        if (UBIQUIA_DEPLOYMENT_NAME.equals(deployment.getMetadata().getName())) {
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
     * Fetches and caches the Ubiquia deployment from Kubernetes, retrying up to
     * {@code maxDeploymentRetries} times with a 1-second delay between attempts.
     *
     * @throws InterruptedException if the retry sleep is interrupted
     * @throws IOException          if all retries are exhausted without a successful response
     */
    private void tryCacheUbiquiaDeploymentFromKubernetes()
        throws InterruptedException, IOException {

        if (this.deploymentRetries >= this.maxDeploymentRetries) {
            throw new IOException("ERROR: Unable to fetch the Ubiquia deployment even after "
                + this.maxDeploymentRetries + " retries...");
        }

        if (Objects.isNull(this.ubiquiaDeployment)) {
            logger.info("...trying to fetch Ubiquia deployment data from Kubernetes...");
            var response = this.deploymentClient.get(UBIQUIA_DEPLOYMENT_NAME);
            if (response.isSuccess()) {
                this.ubiquiaDeployment = response.getObject();
                logger.info("...successfully fetched Ubiquia deployment data from Kubernetes...");
            } else {
                logger.error("Failed to fetch deployment: {}", response.getStatus());
                this.deploymentRetries++;
                logger.info("...waiting and then attempting again...");
                Thread.sleep(1000);
                this.tryCacheUbiquiaDeploymentFromKubernetes();
            }
        }
    }
}
