package org.ubiquia.core.flow.service.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.entity.FlowEventEntity;
import org.ubiquia.common.model.ubiquia.entity.FlowMessageEntity;
import org.ubiquia.common.model.ubiquia.entity.NodeEntity;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.repository.FlowMessageRepository;
import org.ubiquia.core.flow.repository.NodeRepository;

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
     */
    @Transactional
    public void tryQueueMessage(
        FlowEventEntity flowEventEntity,
        final String componentResponse) {

        logger.debug("Received an event with ID {} to queue up for outbox...",
            flowEventEntity.getId());

        var node = this.nodeRepository.findById(flowEventEntity.getNode().getId()).get();

        if (!node.getDownstreamNodes().isEmpty()) {
            this.queueMessagesForNodes(
                flowEventEntity,
                new ArrayList<>(node.getDownstreamNodes()),
                componentResponse);
        } else if (Objects.nonNull(node.getTargetGraph())) {
            var entryNodes = this.nodeRepository
                .findByParentGraphIdAndUpstreamNodesIsEmpty(node.getTargetGraph().getId());
            this.queueMessagesForNodes(flowEventEntity, entryNodes, componentResponse);
        } else {
            logger.debug("No downstream nodes or target graph for node named {}; "
                    + "not creating an outbox message...",
                node.getName());
        }

        this.flowEventRepository.save(flowEventEntity);
        logger.debug("...Completed processing of event.");
    }

    private void queueMessagesForNodes(
        FlowEventEntity flowEventEntity,
        final List<NodeEntity> targets,
        final String componentResponse) {

        var eventTimes = flowEventEntity.getFlowEventTimes();
        eventTimes.setSentToOutboxTime(OffsetDateTime.now());
        for (var target : targets) {
            this.queueMessageForNode(flowEventEntity, target, componentResponse);
        }
        eventTimes.setEventCompleteTime(OffsetDateTime.now());
    }

    private void queueMessageForNode(
        FlowEventEntity flowEventEntity,
        NodeEntity nodeEntity,
        final String payload) {

        logger.debug("Creating an outbox message for event id {} for target node {}",
            flowEventEntity.getId(),
            nodeEntity.getName());

        var message = new FlowMessageEntity();
        message.setFlowEvent(flowEventEntity);
        message.setPayload(payload);
        message.setTags(new HashSet<>());
        message.setTargetNode(nodeEntity);
        message = this.flowMessageRepository.save(message);
        nodeEntity.getOutboxFlowMessages().add(message);
        this.nodeRepository.save(nodeEntity);
        flowEventEntity.getFlowMessages().add(message);
    }
}
