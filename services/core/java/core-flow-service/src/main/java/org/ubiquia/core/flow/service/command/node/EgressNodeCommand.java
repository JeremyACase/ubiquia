package org.ubiquia.core.flow.service.command.node;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import jakarta.transaction.Transactional;
import java.util.Objects;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.ValidationException;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.common.model.ubiquia.entity.FlowEventEntity;
import org.ubiquia.core.flow.component.node.EgressNode;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.repository.FlowMessageRepository;
import org.ubiquia.core.flow.service.builder.FlowEventBuilder;
import org.ubiquia.core.flow.service.orchestrator.NodePayloadOrchestrator;
import org.ubiquia.core.flow.service.telemetry.MicroMeterHelper;
import org.ubiquia.core.flow.service.visitor.validator.PayloadModelValidator;

/**
 * A service that exposes adapter type logic.
 */
@Service
public class EgressNodeCommand implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(EgressNodeCommand.class);

    @Autowired
    private NodePutToComponentCommand nodePutToComponentCommand;

    @Autowired
    private NodePostToComponentCommand nodePostToComponentCommand;

    @Autowired
    private NodePayloadOrchestrator nodePayloadOrchestrator;
    @Autowired
    private FlowEventBuilder flowEventBuilder;
    @Autowired
    private FlowEventRepository flowEventRepository;
    @Autowired
    private FlowMessageRepository flowMessageRepository;
    @Autowired(required = false)
    private MicroMeterHelper microMeterHelper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PayloadModelValidator payloadModelValidator;

    public Logger getLogger() {
        return logger;
    }

    @Transactional
    public void tryProcessInboxMessageFor(
        final FlowMessage flowMessage,
        final EgressNode node) {

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterHelper)) {
            sample = this.microMeterHelper.startSample();
        }

        try {
            var inputPayload = flowMessage.getPayload();
            this.payloadModelValidator
                .tryValidateInputPayloadFor(inputPayload, node);
            var flowEvent = this
                .flowEventBuilder
                .makeEventFrom(flowMessage, node);

            this.tryEgressPayload(flowEvent, node, inputPayload);
        } catch (Exception e) {
            this.getLogger().error("ERROR: Could not process inbox message: {}",
                e.getMessage());
        }
        this.flowMessageRepository.deleteById(flowMessage.getId());

        if (Objects.nonNull(sample)) {
            this.microMeterHelper.endSample(
                sample,
                "tryProcessInboxMessage",
                node.getNodeContext().getTags());
        }
    }

    private void tryEgressPayload(
        final FlowEventEntity flowEventEntity,
        final EgressNode node,
        final String inputPayload)
        throws ValidationException,
        GenerationException,
        JsonProcessingException {

        var outputType = node
            .getNodeContext()
            .getEgressSettings()
            .getHttpOutputType();

        switch (outputType) {

            case PUT: {
                this.nodePutToComponentCommand.tryPutPayloadToComponentSynchronously(
                    flowEventEntity,
                    node,
                    inputPayload);
            }
            break;

            case POST: {
                this.nodePostToComponentCommand.tryPostPayloadToComponentSynchronously(
                    flowEventEntity,
                    node,
                    inputPayload);
            }
            break;

            default: {
                throw new NotImplementedException("ERROR: Unrecognized HTTP output type: "
                    + node.getNodeContext().getEgressSettings().getHttpOutputType());
            }
        }
    }
}
