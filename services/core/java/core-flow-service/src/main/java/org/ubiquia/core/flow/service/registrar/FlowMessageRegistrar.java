package org.ubiquia.core.flow.service.registrar;

import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.common.model.ubiquia.embeddable.FlowEventTimes;
import org.ubiquia.common.model.ubiquia.entity.FlowEntity;
import org.ubiquia.common.model.ubiquia.entity.FlowEventEntity;
import org.ubiquia.common.model.ubiquia.entity.FlowMessageEntity;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.repository.FlowMessageRepository;
import org.ubiquia.core.flow.repository.FlowRepository;
import org.ubiquia.core.flow.repository.GraphRepository;
import org.ubiquia.core.flow.repository.NodeRepository;

/** Registers incoming and sync-received flow messages, creating associated flows and events. */
@Service
public class FlowMessageRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(FlowMessageRegistrar.class);

    @Autowired
    private FlowEventRepository flowEventRepository;

    @Autowired
    private FlowMessageRepository flowMessageRepository;

    @Autowired
    private FlowRepository flowRepository;

    @Autowired
    private GraphRepository graphRepository;

    @Autowired
    private NodeRepository nodeRepository;

    /** Registers an incoming flow message and creates the associated flow and event records. */
    @Transactional
    public void tryRegisterFlowMessage(final FlowMessage dto) {
        var nodeId = dto.getTargetNode().getId();
        var nodeOpt = this.nodeRepository.findById(nodeId);

        if (nodeOpt.isEmpty()) {
            logger.error("Cannot register flow message: no node found with id {}.", nodeId);
            return;
        }

        var nodeEntity = nodeOpt.get();
        var graphEntity = nodeEntity.getGraph();

        var flowEntity = new FlowEntity();
        flowEntity.setGraph(graphEntity);
        flowEntity.setFlowEvents(new HashSet<>());
        graphEntity.getFlows().add(flowEntity);
        this.graphRepository.save(graphEntity);
        flowEntity = this.flowRepository.save(flowEntity);

        var eventTimes = new FlowEventTimes();
        eventTimes.setEventStartTime(OffsetDateTime.now());

        var flowEventEntity = new FlowEventEntity();
        flowEventEntity.setNode(nodeEntity);
        flowEventEntity.setFlow(flowEntity);
        flowEventEntity.setFlowMessages(new HashSet<>());
        flowEventEntity.setInputPayloadStamps(new HashSet<>());
        flowEventEntity.setOutputPayloadStamps(new HashSet<>());
        flowEventEntity.setTags(new HashSet<>());
        flowEventEntity.setFlowEventTimes(eventTimes);
        flowEntity.getFlowEvents().add(flowEventEntity);
        this.flowRepository.save(flowEntity);
        flowEventEntity = this.flowEventRepository.save(flowEventEntity);

        var messageEntity = new FlowMessageEntity();
        messageEntity.setFlowEvent(flowEventEntity);
        messageEntity.setTargetNode(nodeEntity);
        messageEntity.setPayload(dto.getPayload());
        messageEntity.setTags(new HashSet<>());
        this.flowMessageRepository.save(messageEntity);

        logger.info("Registered incoming flow message for node {}.", nodeEntity.getName());
    }

    /** Registers a flow message received during cluster sync if it does not already exist. */
    @Transactional
    public void tryRegisterSync(final FlowMessage dto) {
        if (Objects.nonNull(dto.getId()) && this.flowMessageRepository.existsById(dto.getId())) {
            logger.debug("FlowMessage {} already exists; skipping.", dto.getId());
            return;
        }

        if (Objects.isNull(dto.getFlowEvent()) || Objects.isNull(dto.getFlowEvent().getId())) {
            logger.warn("Cannot sync-register FlowMessage: missing flow event reference.");
            return;
        }
        var flowEventOpt = this.flowEventRepository.findById(dto.getFlowEvent().getId());
        if (flowEventOpt.isEmpty()) {
            logger.warn("Cannot sync-register FlowMessage: FlowEvent {} not found.",
                dto.getFlowEvent().getId());
            return;
        }

        if (Objects.isNull(dto.getTargetNode()) || Objects.isNull(dto.getTargetNode().getId())) {
            logger.warn("Cannot sync-register FlowMessage: missing target node reference.");
            return;
        }
        var nodeOpt = this.nodeRepository.findById(dto.getTargetNode().getId());
        if (nodeOpt.isEmpty()) {
            logger.warn("Cannot sync-register FlowMessage: Node {} not found.",
                dto.getTargetNode().getId());
            return;
        }

        var entity = new FlowMessageEntity();
        if (Objects.nonNull(dto.getId())) {
            entity.setId(dto.getId());
        }
        entity.setFlowEvent(flowEventOpt.get());
        entity.setTargetNode(nodeOpt.get());
        entity.setPayload(dto.getPayload());
        entity.setTags(new HashSet<>());
        this.flowMessageRepository.save(entity);
        logger.info("Sync-registered FlowMessage {}.", entity.getId());
    }
}
