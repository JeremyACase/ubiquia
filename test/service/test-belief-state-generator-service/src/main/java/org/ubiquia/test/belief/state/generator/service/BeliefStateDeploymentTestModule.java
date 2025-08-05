package org.ubiquia.test.belief.state.generator.service;

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
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.api.config.BeliefStateGeneratorServiceConfig;
import org.ubiquia.common.library.implementation.service.builder.BeliefStateNameBuilder;
import org.ubiquia.common.model.ubiquia.embeddable.BeliefStateGeneration;
import org.ubiquia.common.test.helm.service.AbstractHelmTestModule;

@Service
public class BeliefStateDeploymentTestModule extends AbstractHelmTestModule {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateDeploymentTestModule.class);

    @Autowired
    private BeliefStateGeneratorServiceConfig beliefStateGeneratorServiceConfig;

    @Autowired
    private BeliefStateNameBuilder beliefStateNameBuilder;

    @Autowired
    private Cache cache;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    private Boolean beliefStateDeploymentCreated = false;

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

        var generation = new BeliefStateGeneration();
        generation.setDomainName(this.cache.getAcl().getDomain());
        generation.setVersion(this.cache.getAcl().getVersion());

        var postUrl = this.beliefStateGeneratorServiceConfig.getUrl()
            + ":"
            + this.beliefStateGeneratorServiceConfig.getPort()
            + "/belief-state-generator/generate/belief-state";

        logger.info("POSTing to URL: {}", postUrl);

        try {
            var response = this.restTemplate.postForEntity(
                postUrl,
                generation,
                BeliefStateGeneration.class);
        } catch (Exception e) {
            this.testState.addFailure("ERROR: " + e.getMessage());
        }

        try {
            logger.info("...sleeping to give generated Belief State a chance to come alive...");
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (!this.beliefStateDeploymentCreated) {
            var name = this
                .beliefStateNameBuilder
                .getKubernetesBeliefStateNameFrom(this.cache.getAcl());
            this.testState.addFailure("A belief state was never created with name: " + name);
        }

        try {
            logger.info("...generated a belief state for : {}",
                this.objectMapper.writeValueAsString(generation));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
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

        var beliefStateName = this
            .beliefStateNameBuilder
            .getKubernetesBeliefStateNameFrom(this.cache.getAcl());

        // Receive updates from Kubernetes when Deployments change
        informer.addEventHandler(new ResourceEventHandler<>() {

            @Override
            public void onAdd(V1Deployment v1Deployment) {
                if (v1Deployment.getMetadata().getName().equals(beliefStateName)) {
                    beliefStateDeploymentCreated = true;
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

