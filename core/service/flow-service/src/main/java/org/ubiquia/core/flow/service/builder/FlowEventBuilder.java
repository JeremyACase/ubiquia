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
import org.ubiquia.common.model.ubiquia.embeddable.FlowEventTimes;
import org.ubiquia.common.model.ubiquia.entity.FlowEventEntity;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.component.adapter.PollAdapter;
import org.ubiquia.core.flow.repository.AdapterRepository;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.service.visitor.StamperVisitor;

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
     * @return A new Event.
     */
    public FlowEventEntity makeEventFrom(final PollAdapter adapter) {
        var flowEvent = this.getEventHelper(adapter);

        // We haven't received an upstream event in this method; we're assuming it's a new batch
        flowEvent.setBatchId(UUID.randomUUID().toString());

        var eventTimes = flowEvent.getFlowEventTimes();
        eventTimes.setPollStartedTime(OffsetDateTime.now());
        flowEvent = this.flowEventRepository.save(flowEvent);

        return flowEvent;
    }

    public FlowEventEntity makeEventFrom(
        final String inputPayload,
        final AbstractAdapter adapter) throws JsonProcessingException {

        var flowEvent = this.getEventHelper(adapter);

        // We haven't received an upstream event in this method; we're assuming it's a new batch
        flowEvent.setBatchId(UUID.randomUUID().toString());

        var adapterContext = adapter.getAdapterContext();
        if (adapterContext.getAdapterSettings().getPersistInputPayload()) {
            flowEvent.setInputPayload(inputPayload);
        }

        this.stamperVisitor.tryStampInputs(flowEvent, inputPayload);
        flowEvent = this.flowEventRepository.save(flowEvent);

        return flowEvent;
    }

    public FlowEventEntity makeEventFrom(
        final String inputPayload,
        final String batchId,
        final AbstractAdapter adapter) throws Exception {

        var flowEvent = this.getEventHelper(adapter);

        // We have an upstream event from our message; use that event's batch ID.
        flowEvent.setBatchId(batchId);

        var adapterContext = adapter.getAdapterContext();
        if (adapterContext.getAdapterSettings().getPersistInputPayload()) {
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
    private FlowEventEntity getEventHelper(final AbstractAdapter adapter) {

        var adapterContext = adapter.getAdapterContext();
        logger.debug("Creating a new event for adapter: {}",
            adapterContext.getAdapterName());

        var adapterRecord = this.adapterRepository.findById(adapterContext.getAdapterId());

        if (adapterRecord.isEmpty()) {
            throw new RuntimeException("ERROR: Could not find an adapter with id: "
                + adapterContext.getAdapterId());
        }

        var flowEvent = new FlowEventEntity();
        flowEvent.setFlowMessages(new HashSet<>());
        flowEvent.setAdapter(adapterRecord.get());
        flowEvent.setInputPayloadStamps(new HashSet<>());
        flowEvent.setOutputPayloadStamps(new HashSet<>());

        var eventTimes = new FlowEventTimes();
        eventTimes.setEventStartTime(OffsetDateTime.now());
        flowEvent.setFlowEventTimes(eventTimes);

        flowEvent.setTags(new HashSet<>());

        return flowEvent;
    }
}
