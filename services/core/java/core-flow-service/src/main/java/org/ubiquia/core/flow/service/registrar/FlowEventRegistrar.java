package org.ubiquia.core.flow.service.registrar;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.FlowEvent;
import org.ubiquia.common.model.ubiquia.embeddable.KeyValuePair;
import org.ubiquia.common.model.ubiquia.entity.FlowEventEntity;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.repository.FlowRepository;
import org.ubiquia.core.flow.repository.NodeRepository;

/** Registers and persists flow event entities during cluster synchronization. */
@Service
public class FlowEventRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(FlowEventRegistrar.class);

    @Autowired
    private FlowEventRepository flowEventRepository;

    @Autowired
    private FlowRepository flowRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /** Registers a flow event from the given DTO if it does not already exist. */
    @Transactional
    public void tryRegister(final FlowEvent dto) throws Exception {
        if (Objects.nonNull(dto.getId()) && this.flowEventRepository.existsById(dto.getId())) {
            logger.debug("FlowEvent {} already exists; skipping.", dto.getId());
            return;
        }

        if (Objects.isNull(dto.getFlow()) || Objects.isNull(dto.getFlow().getId())) {
            throw new IllegalArgumentException(
                "Cannot register FlowEvent: missing parent flow reference.");
        }
        var flowOpt = this.flowRepository.findById(dto.getFlow().getId());
        if (flowOpt.isEmpty()) {
            throw new IllegalArgumentException(
                "Cannot register FlowEvent: parent Flow " + dto.getFlow().getId() + " not found.");
        }

        if (Objects.isNull(dto.getNode()) || Objects.isNull(dto.getNode().getId())) {
            throw new IllegalArgumentException(
                "Cannot register FlowEvent: missing parent node reference.");
        }
        var nodeOpt = this.nodeRepository.findById(dto.getNode().getId());
        if (nodeOpt.isEmpty()) {
            throw new IllegalArgumentException(
                "Cannot register FlowEvent: parent Node " + dto.getNode().getId() + " not found.");
        }

        var entity = new FlowEventEntity();
        if (Objects.nonNull(dto.getId())) {
            entity.setId(dto.getId());
        }
        entity.setFlow(flowOpt.get());
        entity.setNode(nodeOpt.get());
        entity.setFlowEventTimes(dto.getFlowEventTimes());
        entity.setHttpResponseCode(dto.getHttpResponseCode());
        entity.setFlowMessages(new HashSet<>());
        entity.setInputPayloadStamps(new HashSet<>());
        entity.setOutputPayloadStamps(new HashSet<>());

        if (Objects.nonNull(dto.getInputPayload())) {
            entity.setInputPayload(this.objectMapper.writeValueAsString(dto.getInputPayload()));
        }
        if (Objects.nonNull(dto.getOutputPayload())) {
            entity.setOutputPayload(this.objectMapper.writeValueAsString(dto.getOutputPayload()));
        }

        if (Objects.nonNull(dto.getInputPayloadStamps())) {
            for (var stamp : dto.getInputPayloadStamps()) {
                var kvp = new KeyValuePair();
                kvp.setKey(stamp.getKey());
                kvp.setValue(Objects.nonNull(stamp.getValue())
                    ? this.objectMapper.writeValueAsString(stamp.getValue()) : null);
                entity.getInputPayloadStamps().add(kvp);
            }
        }
        if (Objects.nonNull(dto.getOutputPayloadStamps())) {
            for (var stamp : dto.getOutputPayloadStamps()) {
                var kvp = new KeyValuePair();
                kvp.setKey(stamp.getKey());
                kvp.setValue(Objects.nonNull(stamp.getValue())
                    ? this.objectMapper.writeValueAsString(stamp.getValue()) : null);
                entity.getOutputPayloadStamps().add(kvp);
            }
        }

        this.flowEventRepository.save(entity);
        logger.info("Registered FlowEvent {}.", entity.getId());
    }
}
