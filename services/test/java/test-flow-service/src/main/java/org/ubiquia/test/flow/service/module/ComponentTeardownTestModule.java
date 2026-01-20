package org.ubiquia.test.flow.service.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.embeddable.GraphSettings;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;
import org.ubiquia.common.test.helm.service.AbstractHelmTestModule;

@Service
public class ComponentTeardownTestModule extends AbstractHelmTestModule {

    private static final Logger logger = LoggerFactory.getLogger(ComponentTeardownTestModule.class);
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Logger getLogger() {
        return logger;
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

        var postUrl = this.flowServiceConfig.getUrl()
            + ":"
            + this.flowServiceConfig.getPort()
            + "/ubiquia/core/flow-service/graph/teardown";

        logger.info("POSTing to URL: {}", postUrl);

        try {
            var response = this
                .restTemplate
                .postForEntity(postUrl, graphDeployment, GraphDeployment.class);
            logger.info("Response: {}", this.objectMapper.writeValueAsString(response));

        } catch (Exception e) {
            this.testState.addFailure("ERROR: " + e.getMessage());
        }

        logger.info("...completed.");
    }
}

