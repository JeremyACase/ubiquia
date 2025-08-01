package org.ubiquia.core.flow.service.io.bootstrap;


import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.api.config.BeliefStateGeneratorServiceConfig;
import org.ubiquia.common.model.ubiquia.embeddable.BeliefStateGeneration;
import org.ubiquia.core.flow.component.config.bootstrap.BeliefStateBootstrapConfig;
import org.ubiquia.core.flow.interfaces.InterfaceBootstrapper;
import org.ubiquia.core.flow.service.registrar.AgentCommunicationLanguageRegistrar;

/**
 * This is a service that can "bootstrap" a Ubiquia instance using graphs and agent communication
 * languages from file.
 */
@ConditionalOnProperty(
    value = "ubiquia.agent.flowService.bootstrap.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class BeliefStateBootstrapper implements InterfaceBootstrapper {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateBootstrapper.class);

    @Autowired
    private AgentCommunicationLanguageRegistrar aclRegistrar;

    @Autowired
    private BeliefStateGeneratorServiceConfig beliefStateGeneratorServiceConfig;

    @Value("${ubiquia.kubernetes.enabled}")
    private Boolean kubernetesEnabled;

    @Autowired
    private BeliefStateBootstrapConfig config;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public void bootstrap() throws IOException {

        logger.info("...bootstrapping belief states ...");

        if (!this.kubernetesEnabled) {
            logger.warn("Kubernetes is not enabled, skipping deployment of belief states...");
        } else {
            for (var deployment : this.config.getDeployments()) {
                logger.info("...deploying belief state for domain {} with version: {}",
                    deployment.getDomainName(),
                    deployment.getVersion());

                var url = this.beliefStateGeneratorServiceConfig.getUrl()
                    + ":"
                    + this.beliefStateGeneratorServiceConfig.getPort()
                    + "/belief-state-generator/generate/belief-state";

                var response = this.restTemplate.postForObject(
                    url,
                    deployment,
                    BeliefStateGeneration.class);

                logger.info("...response: {}",
                    this.objectMapper.writeValueAsString(response));
            }
        }

        logger.info("...completed bootstrapping belief states.");
    }
}
