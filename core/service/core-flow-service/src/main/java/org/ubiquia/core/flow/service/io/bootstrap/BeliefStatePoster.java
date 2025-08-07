package org.ubiquia.core.flow.service.io.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.model.ubiquia.embeddable.BeliefStateGeneration;

@Service
public class BeliefStatePoster {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateBootstrapper.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Retryable(
        maxAttempts = 10,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public BeliefStateGeneration postToBeliefState(
        final String url,
        final BeliefStateGeneration deployment) throws Exception {

        logger.info("POSTing to URL... \nURL: {} \nPayload: {}",
            url,
            this.objectMapper.writeValueAsString(deployment));

        var response = this.restTemplate.postForObject(
            url,
            deployment,
            BeliefStateGeneration.class);

        logger.info("Response: {}", this.objectMapper.writeValueAsString(response));

        return response;
    }

    @Recover
    public BeliefStateGeneration recover(
        final Exception e,
        final String url,
        final BeliefStateGeneration deployment) {

        logger.error("All retries failed for POSTing: {}", e.getMessage());
        return null;
    }
}
