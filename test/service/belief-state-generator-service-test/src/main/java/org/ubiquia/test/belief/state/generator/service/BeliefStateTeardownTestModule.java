package org.ubiquia.test.belief.state.generator.service;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.test.helm.service.AbstractHelmTestModule;

@Service
public class BeliefStateTeardownTestModule extends AbstractHelmTestModule {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateTeardownTestModule.class);

    @Autowired
    private Cache cache;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Logger getLogger() {
        return logger;
    }

    private ApiClient apiClient;
    private GenericKubernetesApi<V1Deployment, V1DeploymentList> deploymentClient;
    @Value("${ubiquia.kubernetes.namespace}")
    private String namespace;
    private GenericKubernetesApi<V1Service, V1ServiceList> serviceClient;

    /**
     * Initialize this operator with connections to the Kubernetes API server.
     *
     * @throws IOException Exceptions from not being able to connect to Kubernetes.
     */
    public void init() throws IOException {
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
        this.initializeDeploymentUpdateInformer();
        logger.info("...Kubernetes client connection initialized.");
    }

    @Override
    public void doTests() {

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
            60000L,
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

            }
        });

        factory.startAllRegisteredInformers();
    }
}

