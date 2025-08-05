package org.ubiquia.core.flow.service.command.adapter;

import io.micrometer.core.instrument.Timer;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.library.implementation.service.mapper.FlowEventDtoMapper;
import org.ubiquia.common.model.ubiquia.adapter.QueueAdapterEgress;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.core.flow.component.adapter.QueueAdapter;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.repository.FlowMessageRepository;
import org.ubiquia.core.flow.service.builder.FlowEventBuilder;
import org.ubiquia.core.flow.service.io.Inbox;
import org.ubiquia.core.flow.service.telemetry.MicroMeterHelper;
import org.ubiquia.core.flow.service.visitor.validator.PayloadModelValidator;

/**
 * A service that exposes adapter type logic.
 */
@Service
public class QueueAdapterCommand implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(QueueAdapterCommand.class);
    @Autowired
    private FlowEventBuilder flowEventBuilder;
    @Autowired
    private FlowEventDtoMapper flowEventDtoMapper;
    @Autowired
    private FlowEventRepository flowEventRepository;
    @Autowired
    private FlowMessageRepository flowMessageRepository;
    @Autowired
    private Inbox inbox;
    @Autowired(required = false)
    private MicroMeterHelper microMeterHelper;
    @Autowired
    private PayloadModelValidator payloadModelValidator;

    public Logger getLogger() {
        return logger;
    }

    public ResponseEntity<QueueAdapterEgress> peekFor(final QueueAdapter adapter)
        throws Exception {

        var context = adapter.getAdapterContext();

        this.getLogger()
            .info("Adapter {} for graph {} received a peek request...",
                context.getAdapterName(),
                context.getGraphName());

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterHelper)) {
            sample = this.microMeterHelper.startSample();
        }

        ResponseEntity<QueueAdapterEgress> response = null;
        QueueAdapterEgress egress = null;
        var message = this.inbox.tryQueryInboxMessagesFor(adapter);
        if (Objects.nonNull(message)) {
            this.payloadModelValidator.tryValidateOutputPayloadFor(message.getPayload(), adapter);
            egress = this.getEgressFrom(message, adapter);
        } else {
            egress = this.getEmptyEgress();
        }
        response = ResponseEntity.accepted().body(egress);

        if (Objects.nonNull(sample)) {
            this.microMeterHelper.endSample(sample, "peek", context.getTags());
        }
        return response;
    }

    @Transactional
    public ResponseEntity<QueueAdapterEgress> popFor(final QueueAdapter adapter)
        throws Exception {

        var context = adapter.getAdapterContext();

        this.getLogger()
            .info("Adapter {} for graph {} received a pop request...",
                context.getAdapterName(),
                context.getGraphName());

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterHelper)) {
            sample = this.microMeterHelper.startSample();
        }

        ResponseEntity<QueueAdapterEgress> response = null;
        QueueAdapterEgress egress = null;
        var message = this.inbox.tryQueryInboxMessagesFor(adapter);
        if (Objects.nonNull(message)) {
            this.payloadModelValidator.tryValidateOutputPayloadFor(message.getPayload(), adapter);
            egress = this.getEgressFrom(message, adapter);
            var count = this.flowMessageRepository.countByTargetAdapterId(context.getAdapterId());
            egress.setQueuedRecords(count - 1);
            this.flowMessageRepository.deleteById(message.getId());
        } else {
            egress = this.getEmptyEgress();
        }
        response = ResponseEntity.accepted().body(egress);

        if (Objects.nonNull(sample)) {
            this.microMeterHelper.endSample(sample, "pop", context.getTags());
        }
        return response;
    }

    @Transactional
    private QueueAdapterEgress getEgressFrom(
        final FlowMessage message,
        final QueueAdapter adapter)
        throws Exception {

        var egress = new QueueAdapterEgress();
        var event = this.flowEventBuilder.makeEventFrom(
            message.getPayload(),
            message.getFlowEvent().getBatchId(),
            adapter);
        event.getFlowEventTimes().setPayloadEgressedTime(OffsetDateTime.now());
        event = this.flowEventRepository.save(event);

        var eventDto = this.flowEventDtoMapper.map(event);
        egress.setFlowEvent(eventDto);

        var count = this.flowMessageRepository.countByTargetAdapterId(adapter
            .getAdapterContext()
            .getAdapterId());
        egress.setQueuedRecords(count);
        return egress;
    }

    private QueueAdapterEgress getEmptyEgress() {
        var egress = new QueueAdapterEgress();
        egress.setQueuedRecords(0L);
        return egress;
    }
}
