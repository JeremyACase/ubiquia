package org.ubiquia.core.flow.service.registrar;

import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.HashSet;
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
}
