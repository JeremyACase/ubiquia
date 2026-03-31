package org.ubiquia.test.flow.service.module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.common.test.helm.service.AbstractHelmTestModule;

/**
 * Verifies that the flow service has persisted entities that are candidates for cluster
 * synchronization. This confirms the data layer underpinning {@code ModelSynchronizationService}
 * is functioning correctly in a deployed environment.
 */
@Service
public class FlowClusterSyncTestModule extends AbstractHelmTestModule {

    private static final Logger logger = LoggerFactory.getLogger(FlowClusterSyncTestModule.class);

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
        logger.info("Proceeding with flow cluster sync prerequisite tests...");

        var baseUrl = this.flowServiceConfig.getUrl()
            + ":"
            + this.flowServiceConfig.getPort();

        this.assertDomainOntologiesExist(baseUrl);

        logger.info("...flow cluster sync prerequisite tests completed.");
    }

    private void assertDomainOntologiesExist(final String baseUrl) {
        var url = baseUrl
            + "/ubiquia/core/flow-service/domain-ontology/query/params?page=0&size=1";

        logger.info("Querying for domain ontology sync candidates at: {}", url);

        try {
            var typeRef =
                new ParameterizedTypeReference<GenericPageImplementation<DomainOntology>>() {};
            var response = this.restTemplate.exchange(url, HttpMethod.GET, null, typeRef);
            var page = response.getBody();

            if (page == null || page.getTotalElements() == 0) {
                this.testState.addFailure(
                    "No domain ontology records found in the flow service. "
                        + "At least one must exist as a sync candidate for cluster "
                        + "synchronization to function.");
                return;
            }

            logger.info(
                "Found {} domain ontology record(s) — sync candidates are present.",
                page.getTotalElements());

        } catch (Exception e) {
            this.testState.addFailure(
                "ERROR querying domain ontologies for sync candidates: " + e.getMessage());
        }
    }
}
