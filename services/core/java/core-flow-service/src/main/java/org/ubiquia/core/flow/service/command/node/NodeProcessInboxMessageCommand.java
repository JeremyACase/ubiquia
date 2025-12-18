package org.ubiquia.core.flow.service.command.node;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.core.flow.component.node.AbstractNode;
import org.ubiquia.core.flow.repository.FlowMessageRepository;
import org.ubiquia.core.flow.service.builder.FlowEventBuilder;
import org.ubiquia.core.flow.service.orchestrator.NodePayloadOrchestrator;
import org.ubiquia.core.flow.service.visitor.validator.PayloadModelValidator;

/**
 * A service that exposes various methods common to all adapters.
 */
@Service
@Transactional
public class NodeProcessInboxMessageCommand {

    private static final Logger logger = LoggerFactory.getLogger(NodeProcessInboxMessageCommand.class);

    @Autowired
    private NodePayloadOrchestrator nodePayloadOrchestrator;

    @Autowired
    private FlowEventBuilder flowEventBuilder;

    @Autowired
    private FlowMessageRepository flowMessageRepository;

    @Autowired
    private PayloadModelValidator payloadModelValidator;


    public Logger getLogger() {
        return logger;
    }

    public void tryProcessInboxMessageFor(
        final FlowMessage message,
        final AbstractNode node) {

        try {
            this
                .payloadModelValidator
                .tryValidateInputPayloadFor(message.getPayload(), node);

            var flowEvent = this
                .flowEventBuilder
                .makeEventFrom(message, node);

            this
                .nodePayloadOrchestrator
                .forwardPayload(flowEvent, node, message.getPayload());

        } catch (Exception e) {
            this.getLogger().error("ERROR: {} could not process inbox message: {}",
                node.getNodeContext().getNodeName(),
                e.getMessage());
            this.getLogger().debug("Message payload: {}",
                message.getPayload());
        }

        this.flowMessageRepository.deleteById(message.getId());
    }
}
