package org.ubiquia.core.flow.service.command.node;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.common.model.ubiquia.entity.FlowMessageEntity;
import org.ubiquia.core.flow.component.node.MergeNode;
import org.ubiquia.core.flow.repository.NodeRepository;
import org.ubiquia.core.flow.repository.FlowMessageRepository;
import org.ubiquia.core.flow.service.builder.FlowEventBuilder;
import org.ubiquia.core.flow.service.orchestrator.NodePayloadOrchestrator;
import org.ubiquia.core.flow.service.telemetry.MicroMeterHelper;

/**
 * A service that exposes adapter type logic.
 */
@Service
public class MergeNodeCommand implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(MergeNodeCommand.class);

    @Autowired
    private NodePayloadOrchestrator nodePayloadOrchestrator;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private FlowEventBuilder flowEventBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FlowMessageRepository flowMessageRepository;

    @Autowired(required = false)
    private MicroMeterHelper microMeterHelper;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Transactional
    public void tryProcessMessageFor(final FlowMessage message, final MergeNode adapter) {

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterHelper)) {
            sample = this.microMeterHelper.startSample();
        }

        var nodeContext = adapter.getNodeContext();

        try {
            var messages = this.flowMessageRepository
                .findAllByTargetNodeIdAndFlowEventFlowId(
                    nodeContext.getNodeId(),
                    message.getFlowEvent().getFlow().getId());

            if (!messages.isEmpty()) {

                var targetNodeRecord = this
                    .nodeRepository
                    .findById(nodeContext.getNodeId());
                var targetAdapter = targetNodeRecord.get();

                var upstreamAdapters = targetAdapter.getUpstreamNodes();
                if (messages.size() == upstreamAdapters.size()) {

                    this.getLogger().info("{} has all {} upstream messages "
                            + "with flow id {}; processing...",
                        nodeContext.getNodeName(),
                        messages.size(),
                        message.getFlowEvent().getFlow().getId());

                    var merged = this.mergeMessages(messages);

                    var event = this.flowEventBuilder.makeFlowAndEventFrom(
                        merged,
                        adapter);

                    this.nodePayloadOrchestrator.forwardPayload(event, adapter, merged);

                    this.flowMessageRepository.deleteAll(messages);
                } else {
                    this.getLogger().debug(" {} has only {} messages with batch id {}; "
                            + "not processing...",
                        nodeContext.getNodeName(),
                        messages.size(),
                        message.getFlowEvent().getFlow().getId());
                }
            } else {
                throw new RuntimeException("ERROR: Somehow, the messages list is empty!");
            }
        } catch (Exception e) {
            this.getLogger().error("ERROR: Could not process inbox message: {}",
                e.getMessage());
        }

        if (Objects.nonNull(sample)) {
            this.microMeterHelper.endSample(
                sample,
                "tryProcessInboxMessage",
                nodeContext.getTags());
        }
    }

    private String mergeMessages(final List<FlowMessageEntity> messages)
        throws JsonProcessingException {

        var mergedMap = new HashMap<String, Object>();
        for (var message : messages) {
            var sourceNodeName = message.getFlowEvent().getNode().getName();
            mergedMap.put(sourceNodeName, message.getPayload());
        }

        var mergedString = this.objectMapper.writeValueAsString(mergedMap);
        return mergedString;
    }
}
