package org.ubiquia.core.flow.service.command.adapter;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.repository.FlowMessageRepository;
import org.ubiquia.core.flow.service.builder.FlowEventBuilder;
import org.ubiquia.core.flow.service.orchestrator.AdapterPayloadOrchestrator;
import org.ubiquia.core.flow.service.visitor.validator.PayloadModelValidator;

/**
 * A service that exposes various methods common to all adapters.
 */
@Service
@Transactional
public class AdapterInboxMessageCommand {

    private static final Logger logger = LoggerFactory.getLogger(AdapterInboxMessageCommand.class);

    @Autowired
    private AdapterPayloadOrchestrator adapterPayloadOrchestrator;

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
        final AbstractAdapter adapter) {

        try {
            this.payloadModelValidator.tryValidateInputPayloadFor(
                message.getPayload(),
                adapter);

            var flowEvent = this.flowEventBuilder.makeEventFrom(
                message.getPayload(),
                message.getFlowEvent().getFlowId(),
                adapter);

            this.adapterPayloadOrchestrator.forwardPayload(
                flowEvent,
                adapter,
                message.getPayload());

        } catch (Exception e) {
            this.getLogger().error("ERROR: {} could not process inbox message: {}",
                adapter.getAdapterContext().getAdapterName(),
                e.getMessage());
            this.getLogger().debug("Message payload: {}",
                message.getPayload());
        }

        this.flowMessageRepository.deleteById(message.getId());
    }
}
