package org.ubiquia.test.flow.service.module;

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
import java.io.IOException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.embeddable.GraphSettings;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;
import org.ubiquia.common.test.helm.service.AbstractHelmTestModule;

@Service
public class ComponentDeploymentTestModule extends AbstractHelmTestModule {

    private static final Logger logger = LoggerFactory.getLogger(ComponentDeploymentTestModule.class);

    @Autowired
    private ObjectMapper objectMapper;

    private Boolean dagDeployed = false;

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Autowired
    private RestTemplate restTemplate;

    private ApiClient apiClient;
    private GenericKubernetesApi<V1Deployment, V1DeploymentList> deploymentClient;
    @Value("${ubiquia.kubernetes.namespace}")
    private String namespace;
    private GenericKubernetesApi<V1Service, V1ServiceList> serviceClient;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void doSetup() {
        logger.info("Initializing Kubernetes client connection...");

        try {
            this.apiClient = ClientBuilder.standard().build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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

        this.initializeDeploymentUpdateInformer();
        logger.info("...Kubernetes client connection initialized.");
    }

    @Override
    public void doTests() {
        logger.info("Proceeding with tests...");

        var graphDeployment = new GraphDeployment();
        graphDeployment.setGraphName("pet-store-dag");
        graphDeployment.setDomainOntologyName("pets");

        var version = new SemanticVersion();
        version.setMajor(1);
        version.setMinor(2);
        version.setPatch(3);
        graphDeployment.setDomainVersion(version);

        var graphSettings = new GraphSettings();
        graphSettings.setFlag("devops");
        graphDeployment.setGraphSettings(graphSettings);

        var postUrl = this
            .flowServiceConfig
            .getUrl()
            + ":"
            + this.flowServiceConfig.getPort()
            + "/ubiquia/core/flow-service/graph/deploy";

        logger.info("POSTing to URL: {}", postUrl);

        try {
            var response = this
                .restTemplate
                .postForEntity(postUrl, graphDeployment, GraphDeployment.class);
            logger.info("Response: {}", this.objectMapper.writeValueAsString(response));

        } catch (Exception e) {
            this.testState.addFailure("ERROR: " + e.getMessage());
        }

        try {
            logger.info("...sleeping to give deployed DAG a chance to come alive...");
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (this.dagDeployed) {
            this.testState.addFailure("No components for a pets DAG were created in "
                + "Kubernetes...");
        }

        logger.info("...completed.");
    }

    private void initializeDeploymentUpdateInformer() {
        var factory = new SharedInformerFactory(this.apiClient);
        var informer = factory.sharedIndexInformerFor(
            this.deploymentClient,
            V1Deployment.class,
            60000L,
            this.namespace);

        // Receive updates from Kubernetes when Deployments change
        informer.addEventHandler(new ResourceEventHandler<>() {

            @Override
            public void onAdd(V1Deployment v1Deployment) {

                var labels = v1Deployment.getMetadata().getLabels();
                if (Objects.nonNull(labels)
                    && labels.get("ubiquia-graph").equals("pet-store-dag")) {
                    dagDeployed = true;
                }
            }

            @Override
            public void onUpdate(V1Deployment v1Deployment, V1Deployment apiType1) {

            }

            @Override
            public void onDelete(V1Deployment deployment, boolean b) {

            }
        });

        factory.startAllRegisteredInformers();
    }
}

