package org.ubiquia.core.flow.service.io.broker.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.interfaces.InterfaceBrokerEgress;

/**
 * A class dedicated to publishing payloads over Kafka.
 */
@ConditionalOnProperty(
    value = "ubiquia.broker.kafka.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class KafkaEgress implements InterfaceBrokerEgress {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEgress.class);
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Attempt to publish the payload for the provided adapter.
     *
     * @param payload The payload to publish.
     * @param adapter The adapter to publish a payload for.
     */
    @Override
    public void tryPublishPayload(final String payload, final AbstractAdapter adapter) {
        this.kafkaTemplate.send(
            adapter.getAdapterContext().getBrokerSettings().getTopic(),
            payload);
    }
}
