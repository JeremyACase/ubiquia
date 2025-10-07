package org.ubiquia.core.flow.service.command.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
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
import org.ubiquia.core.flow.component.adapter.EgressAdapter;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.repository.FlowMessageRepository;
import org.ubiquia.core.flow.service.builder.FlowEventBuilder;
import org.ubiquia.core.flow.service.orchestrator.AdapterPayloadOrchestrator;
import org.ubiquia.core.flow.service.telemetry.MicroMeterHelper;
import org.ubiquia.core.flow.service.visitor.validator.PayloadModelValidator;

/**
 * A service that exposes adapter type logic.
 */
@Service
public class EgressAdapterCommand implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(EgressAdapterCommand.class);

    @Autowired
    private AdapterPutToComponentCommand adapterPutToComponentCommand;

    @Autowired
    private AdapterPostToComponentCommand adapterPostToComponentCommand;

    @Autowired
    private AdapterPayloadOrchestrator adapterPayloadOrchestrator;
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

    public void tryProcessInboxMessageFor(
        final FlowMessage flowMessage,
        final EgressAdapter adapter) {

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterHelper)) {
            sample = this.microMeterHelper.startSample();
        }

        try {
            var inputPayload = flowMessage.getPayload();
            this.payloadModelValidator.tryValidateInputPayloadFor(
                inputPayload,
                adapter);
            var flowEvent = this.flowEventBuilder.makeEventFrom(
                inputPayload,
                flowMessage.getFlowEvent().getFlowId(),
                adapter);

            this.tryEgressPayload(flowEvent, adapter, inputPayload);
        } catch (Exception e) {
            this.getLogger().error("ERROR: Could not process inbox message: {}",
                e.getMessage());
        }
        this.flowMessageRepository.deleteById(flowMessage.getId());

        if (Objects.nonNull(sample)) {
            this.microMeterHelper.endSample(
                sample,
                "tryProcessInboxMessage",
                adapter.getAdapterContext().getTags());
        }
    }

    private void tryEgressPayload(
        final FlowEventEntity flowEventEntity,
        final EgressAdapter adapter,
        final String inputPayload)
        throws ValidationException,
        GenerationException,
        JsonProcessingException {

        switch (adapter.getAdapterContext().getEgressSettings().getHttpOutputType()) {

            case PUT: {
                this.adapterPutToComponentCommand.tryPutPayloadToComponentSynchronously(
                    flowEventEntity,
                    adapter,
                    inputPayload);
            }
            break;

            case POST: {
                this.adapterPostToComponentCommand.tryPostPayloadToComponentSynchronously(
                    flowEventEntity,
                    adapter,
                    inputPayload);
            }
            break;

            default: {
                throw new NotImplementedException("ERROR: Unrecognized HTTP output type: "
                    + adapter.getAdapterContext().getEgressSettings().getHttpOutputType());
            }
        }
    }
}
