package org.ubiquia.core.flow.service.builder;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.component.adapter.PollAdapter;
import org.ubiquia.core.flow.model.entity.FlowEvent;
import org.ubiquia.core.flow.repository.AdapterRepository;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.service.visitor.StamperVisitor;

/**
 * A service that can be used to build AMIGOS events.
 */
@Service
@Transactional
public class FlowEventBuilder {

    private static final Logger logger = LoggerFactory.getLogger(FlowEventBuilder.class);
    @Autowired
    private AdapterRepository adapterRepository;
    @Autowired
    private FlowEventRepository flowEventRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StamperVisitor stamperVisitor;

    /**
     * Make an event for a Poll adapter.
     *
     * @param adapter The adapter to use to make an event.
     * @return A new AMIGOS Event.
     */
    public FlowEvent makeEventFrom(final PollAdapter adapter) {
        var amigosEvent = this.getEventHelper(adapter);

        // We haven't received an upstream event in this method; we're assuming it's a new batch
        amigosEvent.setBatchId(UUID.randomUUID().toString());
        amigosEvent.setPollStartedTime(OffsetDateTime.now());
        amigosEvent.setPayloadReceivedTime(null);
        amigosEvent = this.flowEventRepository.save(amigosEvent);

        return amigosEvent;
    }

    /**
     * Make a new event from an input payload for an adapter.
     *
     * @param inputPayload The input payload to make an event for.
     * @param adapter      The adapter to build an event for.
     * @return A new AMIGOS event.
     * @throws JsonProcessingException Exceptions from processing JSON.
     */
    public FlowEvent makeEventFrom(
        final String inputPayload,
        final AbstractAdapter adapter) throws JsonProcessingException {

        var flowEvent = this.getEventHelper(adapter);

        // We haven't received an upstream event in this method; we're assuming it's a new batch
        flowEvent.setBatchId(UUID.randomUUID().toString());

        var adapterContext = adapter.getAdapterContext();
        if (adapterContext.getAdapterSettings().getIsPersistInputPayload()) {
            flowEvent.setInputPayload(inputPayload);
        }

        this.stamperVisitor.tryStampInputs(flowEvent, inputPayload);
        flowEvent = this.flowEventRepository.save(flowEvent);

        return flowEvent;
    }

    /**
     * Make a new event from an input payload for an adapter.
     *
     * @param inputPayload The input payload to make an event for.
     * @param batchId      The batch id to set for this event.
     * @param adapter      The adapter to build an event for.
     * @return A new AMIGOS event.
     * @throws JsonProcessingException Exceptions from processing JSON.
     */
    public FlowEvent makeEventFrom(
        final String inputPayload,
        final String batchId,
        final AbstractAdapter adapter) throws Exception {

        var flowEvent = this.getEventHelper(adapter);

        // We have an upstream event from our message; use that event's batch ID.
        flowEvent.setBatchId(batchId);

        var adapterContext = adapter.getAdapterContext();
        if (adapterContext.getAdapterSettings().getIsPersistInputPayload()) {
            flowEvent.setInputPayload(inputPayload);
        }

        this.stamperVisitor.tryStampInputs(flowEvent, inputPayload);
        flowEvent = this.flowEventRepository.save(flowEvent);

        return flowEvent;
    }

    /**
     * A simple helper method to create events.
     *
     * @param adapter The adapter we're using to build an event for.
     * @return A new event.
     */
    private FlowEvent getEventHelper(final AbstractAdapter adapter) {

        var adapterContext = adapter.getAdapterContext();
        logger.debug("Creating a new event for adapter with ID: {}",
            adapterContext.getAdapterId());

        var adapterRecord = this.adapterRepository.findById(adapterContext.getAdapterId());

        if (adapterRecord.isEmpty()) {
            throw new RuntimeException("ERROR: Could not find an adapter with id: "
                + adapterContext.getAdapterId());
        }

        var flowEvent = new FlowEvent();
        flowEvent.setFlowMessages(new HashSet<>());
        flowEvent.setAdapter(adapterRecord.get());
        flowEvent.setInputPayloadStamps(new HashSet<>());
        flowEvent.setOutputPayloadStamps(new HashSet<>());

        flowEvent.setEventStartTime(OffsetDateTime.now());
        flowEvent.setTags(new HashSet<>());

        return flowEvent;
    }
}
