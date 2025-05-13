package org.ubiquia.core.flow.service.logic.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.adapter.EgressAdapter;
import org.ubiquia.core.flow.interfaces.InterfaceLogger;
import org.ubiquia.core.flow.model.dto.FlowMessageDto;
import org.ubiquia.core.flow.model.enums.EgressType;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.repository.FlowMessageRepository;
import org.ubiquia.core.flow.service.builder.FlowEventBuilder;
import org.ubiquia.core.flow.service.orchestrator.AdapterPayloadOrchestrator;
import org.ubiquia.core.flow.service.visitor.validator.PayloadModelValidator;
import org.ubiquia.core.flow.service.telemetry.MicroMeterHelper;

/**
 * A service that exposes adapter type logic.
 */
@Service
public class EgressAdapterLogic implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(EgressAdapterLogic.class);
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

    @Transactional
    public void tryProcessInboxMessageFor(
        final FlowMessageDto flowMessage,
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
                flowMessage.getFlowEvent().getBatchId(),
                adapter);

            flowEvent.getFlowEventTimes().setPayloadReceivedTime(OffsetDateTime.now());

            ResponseEntity<Object> response = null;
            var context = adapter.getAdapterContext();
            if (context.getEgressSettings().getEgressType().equals(EgressType.PUT)) {
                response = this.adapterPayloadOrchestrator.tryPostPayloadToAgentSynchronously(
                    flowEvent,
                    adapter,
                    inputPayload);
            } else {
                response = this.adapterPayloadOrchestrator.tryPutPayloadToAgentSynchronously(
                    flowEvent,
                    adapter,
                    inputPayload);
            }
            flowEvent.getFlowEventTimes().setEgressResponseReceivedTime(OffsetDateTime.now());
            flowEvent.setHttpResponseCode(response.getStatusCode().value());

            this.payloadModelValidator.tryValidateOutputPayloadFor(
                this.objectMapper.writeValueAsString(response.getBody()),
                adapter);
            this.getLogger().info("Received response: {}", response.getStatusCode());
            this.flowEventRepository.save(flowEvent);
            this.flowMessageRepository.deleteById(flowMessage.getId());
        } catch (Exception e) {
            this.getLogger().error("ERROR: Could not process inbox message: {}",
                e.getMessage());
        }

        if (Objects.nonNull(sample)) {
            this.microMeterHelper.endSample(
                sample,
                "tryProcessInboxMessage",
                adapter.getAdapterContext().getTags());
        }
    }
}
