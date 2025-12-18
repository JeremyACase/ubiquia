package org.ubiquia.core.flow.service.builder;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Flow;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.common.model.ubiquia.embeddable.FlowEventTimes;
import org.ubiquia.common.model.ubiquia.entity.FlowEntity;
import org.ubiquia.common.model.ubiquia.entity.FlowEventEntity;
import org.ubiquia.core.flow.component.node.AbstractNode;
import org.ubiquia.core.flow.component.node.MergeNode;
import org.ubiquia.core.flow.component.node.PollNode;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.repository.FlowRepository;
import org.ubiquia.core.flow.repository.NodeRepository;
import org.ubiquia.core.flow.service.visitor.StamperVisitor;

@Service
public class FlowEventBuilder {

    private static final Logger logger = LoggerFactory.getLogger(FlowEventBuilder.class);
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private FlowBuilder flowBuilder;
    @Autowired
    private FlowRepository flowRepository;
    @Autowired
    private FlowEventRepository flowEventRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StamperVisitor stamperVisitor;

    /**
     * Make an event for a Poll adapter.
     *
     * @param node The node to use to make an event.
     * @return A new Event.
     */
    @Transactional
    public FlowEventEntity makeFlowAndEventFrom(final PollNode node) {

        var flowEntity = this.flowBuilder.makeFlowFrom(node);

        var flowEvent = this.getEventHelper(node);

        // Because poll node
        flowEvent
            .getFlowEventTimes()
            .setPollStartedTime(OffsetDateTime.now());

        flowEvent.setFlow(flowEntity);
        flowEntity.getFlowEvents().add(flowEvent);
        this.flowRepository.save(flowEntity);

        flowEvent = this.flowEventRepository.save(flowEvent);

        return flowEvent;
    }

    @Transactional
    public FlowEventEntity makeFlowAndEventFrom(
        final String inputPayload,
        final AbstractNode node)
        throws JsonProcessingException {

        var flowEvent = this.getEventHelper(node);

        var nodeContext = node.getNodeContext();
        if (nodeContext.getNodeSettings().getPersistInputPayload()) {
            flowEvent.setInputPayload(inputPayload);
        }

        this.stamperVisitor.tryStampInputs(flowEvent, inputPayload);

        var flowEntity = this.flowBuilder.makeFlowFrom(node);
        flowEvent.setFlow(flowEntity);
        flowEntity.getFlowEvents().add(flowEvent);
        this.flowRepository.save(flowEntity);

        flowEvent = this.flowEventRepository.save(flowEvent);

        return flowEvent;
    }

    @Transactional
    public FlowEventEntity makeEventFrom(
        final String mergedPayload,
        final String flowId,
        final MergeNode node)
        throws Exception {

        var flowEvent = this.getEventHelper(node);

        var flowRecord = this.flowRepository.findById(flowId);
        if (flowRecord.isEmpty()) {
            throw new IllegalArgumentException("ERROR: Could not flow with ID: "
                + flowId);
        }

        var flowEntity = flowRecord.get();

        flowEvent.setFlow(flowEntity);
        flowEntity.getFlowEvents().add(flowEvent);
        this.flowRepository.save(flowEntity);

        var nodeContext = node.getNodeContext();
        if (nodeContext.getNodeSettings().getPersistInputPayload()) {
            flowEvent.setInputPayload(mergedPayload);
        }

        this.stamperVisitor.tryStampInputs(flowEvent, mergedPayload);
        flowEvent = this.flowEventRepository.save(flowEvent);

        return flowEvent;
    }

    @Transactional
    public FlowEventEntity makeEventFrom(
        final FlowMessage flowMessage,
        final AbstractNode node)
        throws Exception {

        var flowEvent = this.getEventHelper(node);

        var flow = flowMessage.getFlowEvent().getFlow();
        var flowRecord = this.flowRepository.findById(flow.getId());

        if (flowRecord.isEmpty()) {
            throw new IllegalArgumentException("ERROR: Could not flow with ID: "
                + flow.getId());
        }

        var flowEntity = flowRecord.get();

        flowEvent.setFlow(flowEntity);
        flowEntity.getFlowEvents().add(flowEvent);
        this.flowRepository.save(flowEntity);

        var inputPayload = flowMessage.getPayload();

        var nodeContext = node.getNodeContext();
        if (nodeContext.getNodeSettings().getPersistInputPayload()) {
            flowEvent.setInputPayload(inputPayload);
        }

        this.stamperVisitor.tryStampInputs(flowEvent, inputPayload);
        flowEvent = this.flowEventRepository.save(flowEvent);

        return flowEvent;
    }

    /**
     * A simple helper method to create events.
     *
     * @param node The adapter we're using to build an event for.
     * @return A new event.
     */
    private FlowEventEntity getEventHelper(final AbstractNode node) {

        var nodeContext = node.getNodeContext();
        logger.debug("Creating a new event for node: {}", nodeContext.getNodeName());

        var nodeRecord = this.nodeRepository.findById(nodeContext.getNodeId());

        if (nodeRecord.isEmpty()) {
            throw new RuntimeException("ERROR: Could not find an node with id: "
                + nodeContext.getNodeId());
        }

        var flowEvent = new FlowEventEntity();
        flowEvent.setFlowMessages(new HashSet<>());
        flowEvent.setNode(nodeRecord.get());
        flowEvent.setInputPayloadStamps(new HashSet<>());
        flowEvent.setOutputPayloadStamps(new HashSet<>());

        var eventTimes = new FlowEventTimes();
        eventTimes.setEventStartTime(OffsetDateTime.now());
        flowEvent.setFlowEventTimes(eventTimes);

        flowEvent.setTags(new HashSet<>());

        return flowEvent;
    }
}
