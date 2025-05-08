package org.ubiquia.core.flow.service.builder;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.UUID;

import org.machina.core.amigos.adapter.AAdapter;
import org.machina.core.amigos.adapter.PollAdapter;
import org.machina.core.amigos.model.entity.AmigosEvent;
import org.machina.core.amigos.repository.AdapterRepository;
import org.machina.core.amigos.repository.AmigosEventRepository;
import org.machina.core.amigos.service.logic.Stamper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A service that can be used to build AMIGOS events.
 */
@Service
@Transactional
public class AmigosEventBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AmigosEventBuilder.class);
    @Autowired
    private AdapterRepository adapterRepository;
    @Autowired
    private AmigosEventRepository amigosEventRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Stamper stamper;

    /**
     * Make an event for a Poll adapter.
     *
     * @param adapter The adapter to use to make an event.
     * @return A new AMIGOS Event.
     */
    public AmigosEvent makeEventFrom(final PollAdapter adapter) {
        var amigosEvent = this.getEventHelper(adapter);

        // We haven't received an upstream event in this method; we're assuming it's a new batch
        amigosEvent.setBatchId(UUID.randomUUID().toString());
        amigosEvent.setPollStartedTime(OffsetDateTime.now());
        amigosEvent.setPayloadReceivedTime(null);
        amigosEvent = this.amigosEventRepository.save(amigosEvent);

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
    public AmigosEvent makeEventFrom(
        final String inputPayload,
        final AAdapter adapter) throws JsonProcessingException {

        var amigosEvent = this.getEventHelper(adapter);

        // We haven't received an upstream event in this method; we're assuming it's a new batch
        amigosEvent.setBatchId(UUID.randomUUID().toString());

        if (adapter.getAdapterSettings().getIsPersistInputPayload()) {
            amigosEvent.setInputPayload(inputPayload);
        }

        this.stamper.tryStampInputs(amigosEvent, inputPayload);
        amigosEvent = this.amigosEventRepository.save(amigosEvent);

        return amigosEvent;
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
    public AmigosEvent makeEventFrom(
        final String inputPayload,
        final String batchId,
        final AAdapter adapter) throws Exception {

        var amigosEvent = this.getEventHelper(adapter);

        // We have an upstream event from our message; use that event's batch ID.
        amigosEvent.setBatchId(batchId);

        if (adapter.getAdapterSettings().getIsPersistInputPayload()) {
            amigosEvent.setInputPayload(inputPayload);
        }

        this.stamper.tryStampInputs(amigosEvent, inputPayload);
        amigosEvent = this.amigosEventRepository.save(amigosEvent);

        return amigosEvent;
    }

    /**
     * A simple helper method to create events.
     *
     * @param adapter The adapter we're using to build an event for.
     * @return A new event.
     */
    private AmigosEvent getEventHelper(final AAdapter adapter) {

        logger.debug("Creating a new event for adapter with ID: {}", adapter.getAdapterId());

        var adapterRecord = this.adapterRepository.findById(adapter.getAdapterId());

        if (adapterRecord.isEmpty()) {
            throw new RuntimeException("ERROR: Could not find an adapter with id: "
                + adapter.getAdapterId());
        }

        var amigosEvent = new AmigosEvent();
        amigosEvent.setAmigosMessages(new HashSet<>());
        amigosEvent.setAdapter(adapterRecord.get());
        amigosEvent.setInputPayloadStamps(new HashSet<>());
        amigosEvent.setOutputPayloadStamps(new HashSet<>());

        amigosEvent.setEventStartTime(OffsetDateTime.now());
        amigosEvent.setTags(new HashSet<>());

        return amigosEvent;
    }
}
