package org.ubiquia.core.flow.service.command.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.model.entity.FlowEvent;
import org.ubiquia.core.flow.service.io.Outbox;
import org.ubiquia.core.flow.service.visitor.StamperVisitor;
import org.ubiquia.core.flow.service.visitor.validator.PayloadModelValidator;

/**
 * A service that exposes various methods common to all adapters.
 */
@Service
public class AdapterAgentResponseCommand {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Outbox outbox;

    @Autowired
    private PayloadModelValidator payloadModelValidator;

    @Autowired
    private StamperVisitor stamperVisitor;

    public void processAgentResponse(
        final FlowEvent flowEvent,
        final AbstractAdapter adapter,
        final ResponseEntity<Object> response)
        throws JsonProcessingException, ValidationException, GenerationException {

        var stringifiedPayload = this.objectMapper.writeValueAsString(response.getBody());
        this.payloadModelValidator.tryValidateOutputPayloadFor(stringifiedPayload, adapter);
        this.stamperVisitor.tryStampOutputs(flowEvent, stringifiedPayload);

        if (adapter.getAdapterContext().getAdapterSettings().getIsPersistOutputPayload()) {
            flowEvent.setOutputPayload(stringifiedPayload);
        }
        flowEvent.getFlowEventTimes().setEventCompleteTime(OffsetDateTime.now());
        this.outbox.tryQueueAgentResponse(flowEvent, stringifiedPayload);
    }
}
