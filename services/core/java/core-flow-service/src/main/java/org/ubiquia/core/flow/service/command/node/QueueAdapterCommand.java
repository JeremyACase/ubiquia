package org.ubiquia.core.flow.service.command.node;

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
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.common.model.ubiquia.node.QueueNodeEgress;
import org.ubiquia.core.flow.component.node.QueueNode;
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

    public ResponseEntity<QueueNodeEgress> peekFor(final QueueNode node)
        throws Exception {

        var context = node.getNodeContext();

        this.getLogger()
            .info("Node {} for graph {} received a peek request...",
                context.getNodeName(),
                context.getGraphName());

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterHelper)) {
            sample = this.microMeterHelper.startSample();
        }

        ResponseEntity<QueueNodeEgress> response = null;
        QueueNodeEgress egress = null;
        var message = this.inbox.tryQueryInboxMessagesFor(node);
        if (Objects.nonNull(message)) {
            this.payloadModelValidator.tryValidateOutputPayloadFor(message.getPayload(), node);
            egress = this.getEgressFrom(message, node);
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
    public ResponseEntity<QueueNodeEgress> popFor(final QueueNode node)
        throws Exception {

        var context = node.getNodeContext();

        this.getLogger()
            .info("Node {} for graph {} received a pop request...",
                context.getNodeName(),
                context.getGraphName());

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterHelper)) {
            sample = this.microMeterHelper.startSample();
        }

        ResponseEntity<QueueNodeEgress> response = null;
        QueueNodeEgress egress = null;
        var message = this.inbox.tryQueryInboxMessagesFor(node);
        if (Objects.nonNull(message)) {
            this.payloadModelValidator.tryValidateOutputPayloadFor(message.getPayload(), node);
            egress = this.getEgressFrom(message, node);
            var count = this.flowMessageRepository.countByTargetNodeId(context.getNodeId());
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
    private QueueNodeEgress getEgressFrom(
        final FlowMessage message,
        final QueueNode node)
        throws Exception {

        var egress = new QueueNodeEgress();
        var event = this.flowEventBuilder.makeFlowAndEventFrom(
            message.getPayload(),
            node);
        event.getFlowEventTimes().setPayloadEgressedTime(OffsetDateTime.now());
        event = this.flowEventRepository.save(event);

        var eventDto = this.flowEventDtoMapper.map(event);
        egress.setFlowEvent(eventDto);

        var count = this.flowMessageRepository.countByTargetNodeId(node
            .getNodeContext()
            .getNodeId());
        egress.setQueuedRecords(count);
        return egress;
    }

    private QueueNodeEgress getEmptyEgress() {
        var egress = new QueueNodeEgress();
        egress.setQueuedRecords(0L);
        return egress;
    }
}
