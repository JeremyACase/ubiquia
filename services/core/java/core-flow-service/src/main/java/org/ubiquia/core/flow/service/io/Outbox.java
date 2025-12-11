package org.ubiquia.core.flow.service.io;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.entity.FlowEventEntity;
import org.ubiquia.common.model.ubiquia.entity.FlowMessageEntity;
import org.ubiquia.core.flow.repository.NodeRepository;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.repository.FlowMessageRepository;

/**
 * This is a service dedicated to sending message to the database on behalf of adapters so that
 * downstream adapters can, in turn, consume the messages.
 */
@Service
public class Outbox {

    private static final Logger logger = LoggerFactory.getLogger(Outbox.class);
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private FlowEventRepository flowEventRepository;
    @Autowired
    private FlowMessageRepository flowMessageRepository;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Queue up a response from a component so that a downstream adapter may
     * consume it.
     *
     * @param flowEventEntity   The event associated with the response.
     * @param componentResponse The response from the agent.
     * @throws JsonProcessingException Exceptions from processing payloads.
     */
    @Transactional
    public void tryQueueMessage(
        FlowEventEntity flowEventEntity,
        final String componentResponse) {

        logger.debug("Received an event with ID {} to queue up for outbox...",
            flowEventEntity.getId());

        var targetAdapterRecord = this
            .nodeRepository
            .findById(flowEventEntity.getNode().getId());

        if (!targetAdapterRecord.get().getDownstreamNodes().isEmpty()) {

            var eventTimes = flowEventEntity.getFlowEventTimes();
            eventTimes.setSentToOutboxTime(OffsetDateTime.now());
            for (var adapter : targetAdapterRecord.get().getDownstreamNodes()) {
                logger.debug("Creating an outbox message for event id {} for target node {}",
                    flowEventEntity.getId(),
                    adapter.getName());
                var message = new FlowMessageEntity();
                message.setFlowEvent(flowEventEntity);
                message.setPayload(componentResponse);
                message.setTags(new HashSet<>());
                message.setTargetNode(adapter);
                message = this.flowMessageRepository.save(message);
                adapter.getOutboxFlowMessages().add(message);
                this.nodeRepository.save(adapter);
                flowEventEntity.getFlowMessages().add(message);
            }
            eventTimes.setEventCompleteTime(OffsetDateTime.now());
        } else {
            logger.debug("No downstream nodes for nodes named {}; not creating "
                    + "an outbox message...",
                flowEventEntity.getNode().getName());
        }
        this.flowEventRepository.save(flowEventEntity);
        logger.debug("...Completed processing of event.");
    }
}
