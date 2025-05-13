package org.ubiquia.core.flow.service.io.broker;

import java.time.OffsetDateTime;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.model.entity.FlowEvent;
import org.ubiquia.core.flow.service.io.broker.kafka.KafkaEgress;

/**
 * A service that can be used to publish events over brokers.
 */
@Service
public class BrokerEgress {

    private static final Logger logger = LoggerFactory.getLogger(BrokerEgress.class);
    @Autowired(required = false)
    private KafkaEgress kafkaEgress;

    /**
     * Attempt to publish an event over an adapter's configured broker.
     *
     * @param flowEvent The event to try publishing.
     * @param payload   The payload to publish.
     * @param adapter   The adapter to publish a message for.
     */
    public void tryPublish(
        FlowEvent flowEvent,
        final String payload,
        final AbstractAdapter adapter) {

        var adapterContext = adapter.getAdapterContext();
        logger.debug("Got a request to publish the output of an event for adapter of graph {} "
                + " and agent {}",
            adapterContext.getGraphName(),
            adapterContext.getAgentName());


        switch (adapterContext.getBrokerSettings().getType()) {

            case KAFKA: {
                if (Objects.isNull(this.kafkaEgress)) {
                    throw new RuntimeException("ERROR: Cannot egress a payload over Kafka when "
                        + " Kafka isn't enabled!");
                }
                flowEvent.setPayloadEgressedTime(OffsetDateTime.now());
                this.kafkaEgress.tryPublishPayload(payload, adapter);
            }
            break;

            default: {
                throw new RuntimeException("ERROR: No broker configured for adapter!");
            }
        }
    }
}
