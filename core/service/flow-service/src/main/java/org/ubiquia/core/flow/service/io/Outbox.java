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
import org.ubiquia.common.model.ubiquia.entity.FlowEvent;
import org.ubiquia.common.model.ubiquia.entity.FlowMessage;
import org.ubiquia.core.flow.repository.AdapterRepository;
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
    private AdapterRepository adapterRepository;
    @Autowired
    private FlowEventRepository flowEventRepository;
    @Autowired
    private FlowMessageRepository flowMessageRepository;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Queue up a response from an agent so that a downstream adapter may
     * consume it.
     *
     * @param flowEvent     The event associated with the response.
     * @param agentResponse The response from the agent.
     * @throws JsonProcessingException Exceptions from processing payloads.
     */
    @Transactional
    public void tryQueueAgentResponse(
        FlowEvent flowEvent,
        final String agentResponse) {

        logger.debug("Received an event with ID {} to queue up for outbox...",
            flowEvent.getId());

        var targetAdapterRecord = this.adapterRepository.findById(flowEvent.getAdapter().getId());
        if (!targetAdapterRecord.get().getDownstreamAdapters().isEmpty()) {

            var eventTimes = flowEvent.getFlowEventTimes();
            eventTimes.setSentToOutboxTime(OffsetDateTime.now());
            for (var adapter : targetAdapterRecord.get().getDownstreamAdapters()) {
                logger.debug("Creating an outbox message for event id {} for target adapter {}",
                    flowEvent.getId(),
                    adapter.getAdapterName());
                var message = new FlowMessage();
                message.setFlowEvent(flowEvent);
                message.setPayload(agentResponse);
                message.setTags(new HashSet<>());
                message.setTargetAdapter(adapter);
                message = this.flowMessageRepository.save(message);
                adapter.getOutboxMessages().add(message);
                this.adapterRepository.save(adapter);
                flowEvent.getFlowMessages().add(message);
            }
            eventTimes.setEventCompleteTime(OffsetDateTime.now());
            this.flowEventRepository.save(flowEvent);
        } else {
            logger.debug("No downstream adapters for adapter with id {}; not creating "
                    + "an outbox message...",
                flowEvent.getAdapter().getId());
        }

        logger.debug("...Completed processing of event.");
    }
}
