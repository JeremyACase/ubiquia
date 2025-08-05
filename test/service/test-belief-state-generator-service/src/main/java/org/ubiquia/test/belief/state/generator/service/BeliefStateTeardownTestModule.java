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
import java.time.Duration;
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
public class BeliefStateTeardownTestModule extends AbstractHelmTestModule {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateTeardownTestModule.class);

    @Autowired
    private BeliefStateGeneratorServiceConfig beliefStateGeneratorServiceConfig;

    @Autowired
    private BeliefStateNameBuilder beliefStateNameBuilder;

    private Boolean beliefStateDeploymentTornDown = false;

    @Autowired
    private Cache cache;

    @Autowired
    private ObjectMapper objectMapper;

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

    /**
     * Initialize this operator with connections to the Kubernetes API server.
     *
     * @throws IOException Exceptions from not being able to connect to Kubernetes.
     */
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
        if (this.testState.getPassed()) {

            try {
                logger.info("...sleeping to give resources time to settle...");
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            logger.info("Proceeding with tests...");

            var generation = new BeliefStateGeneration();
            generation.setDomainName(this.cache.getAcl().getDomain());
            generation.setVersion(this.cache.getAcl().getVersion());

            var postUrl = this.beliefStateGeneratorServiceConfig.getUrl()
                + ":"
                + this.beliefStateGeneratorServiceConfig.getPort()
                + "/belief-state-generator/teardown/belief-state";

            logger.info("...POSTing to teardown at: {}", postUrl);

            var response = this.restTemplate.postForEntity(
                postUrl,
                generation,
                BeliefStateGeneration.class);

            var name = this
                .beliefStateNameBuilder
                .getKubernetesBeliefStateNameFrom(this.cache.getAcl());

            this.waitForBeliefStateTearDown(name, Duration.ofSeconds(60));

            logger.info("...completed.");
        } else {
            logger.info("Tests FAILED; not tearing down belief state so it can be diagnosed...");
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
            60000L,
            this.namespace);

        var beliefStateName = this
            .beliefStateNameBuilder
            .getKubernetesBeliefStateNameFrom(this.cache.getAcl());

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
                if (deployment.getMetadata().getName().equals(beliefStateName)) {
                    beliefStateDeploymentTornDown = true;
                }
            }
        });

        factory.startAllRegisteredInformers();
    }

    /**
     * Because threads are fun.
     *
     * @param beliefStateName The belief state we're checking for.
     * @param timeout         The timeout before we simply give up.
     */
    private void waitForBeliefStateTearDown(
        final String beliefStateName,
        final Duration timeout) {
        var startTime = System.currentTimeMillis();
        var timeoutMs = timeout.toMillis();

        while (!beliefStateDeploymentTornDown
            && (System.currentTimeMillis() - startTime) < timeoutMs) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for belief state teardown");
            }
        }

        if (!beliefStateDeploymentTornDown) {
            this.testState.addFailure("A belief state was never torn down with name: "
                + beliefStateName);
        } else {
            logger.info("...tore down a belief state for : {}", beliefStateName);
        }
    }
}

