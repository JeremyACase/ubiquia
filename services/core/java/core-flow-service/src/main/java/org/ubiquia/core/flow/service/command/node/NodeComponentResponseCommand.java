package org.ubiquia.core.flow.service.command.node;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.entity.FlowEventEntity;
import org.ubiquia.core.flow.component.node.AbstractNode;
import org.ubiquia.core.flow.service.io.Outbox;
import org.ubiquia.core.flow.service.visitor.StamperVisitor;
import org.ubiquia.core.flow.service.visitor.validator.PayloadModelValidator;

/**
 * A service that exposes various methods common to all adapters.
 */
@Service
public class NodeComponentResponseCommand {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Outbox outbox;

    @Autowired
    private PayloadModelValidator payloadModelValidator;

    @Autowired
    private StamperVisitor stamperVisitor;

    public void processComponentResponse(
        final FlowEventEntity flowEventEntity,
        final AbstractNode node,
        final ResponseEntity<Object> response)
        throws JsonProcessingException {

        var success = true;
        var stringifiedPayload = this.objectMapper.writeValueAsString(response.getBody());

        try {
            this.payloadModelValidator.tryValidateOutputPayloadFor(
                stringifiedPayload,
                node);
        } catch (Exception e) {
            success = false;
        }

        this.stamperVisitor.tryStampOutputs(flowEventEntity, stringifiedPayload);

        if (node.getNodeContext().getNodeSettings().getPersistOutputPayload()) {
            flowEventEntity.setOutputPayload(stringifiedPayload);
        }
        flowEventEntity.getFlowEventTimes().setEventCompleteTime(OffsetDateTime.now());

        if (success) {
            this.outbox.tryQueueMessage(flowEventEntity, stringifiedPayload);
        }
    }
}
