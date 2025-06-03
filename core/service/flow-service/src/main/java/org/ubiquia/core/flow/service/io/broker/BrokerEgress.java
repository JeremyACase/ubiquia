package org.ubiquia.core.flow.service.io.broker;

import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.adapter.PublishAdapter;
import org.ubiquia.common.model.ubiquia.dto.FlowMessageDto;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.service.io.broker.kafka.KafkaEgress;

/**
 * A service that can be used to publish events over brokers.
 */
@Service
public class BrokerEgress {

    private static final Logger logger = LoggerFactory.getLogger(BrokerEgress.class);
    @Autowired
    private FlowEventRepository flowEventRepository;
    @Autowired(required = false)
    private KafkaEgress kafkaEgress;

    @Transactional
    public void tryPublishFor(
        FlowMessageDto flowMessage,
        final PublishAdapter adapter) {

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
                var egressTime = OffsetDateTime.now();
                this.kafkaEgress.tryPublishPayload(flowMessage.getPayload(), adapter);
                this.egressHelper(flowMessage, egressTime);
            }
            break;

            default: {
                throw new RuntimeException("ERROR: No broker configured for adapter!");
            }
        }
    }

    private void egressHelper(
        final FlowMessageDto flowMessage,
        final OffsetDateTime egressTime) {

        var flowEventRecord = this
            .flowEventRepository
            .findById(flowMessage.getFlowEvent().getId());

        if (flowEventRecord.isEmpty()) {
            throw new RuntimeException("ERROR: Could not find an event for id: "
                + flowMessage.getFlowEvent().getId());
        }

        var times = flowEventRecord.get().getFlowEventTimes();
        times.setPayloadEgressedTime(egressTime);
        times.setEventCompleteTime(OffsetDateTime.now());
        this.flowEventRepository.save(flowEventRecord.get());
    }
}
