package org.ubiquia.core.flow.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.model.dto.AdapterDto;
import org.ubiquia.core.flow.model.dto.FlowEventDto;
import org.ubiquia.core.flow.model.dto.KeyValuePairDto;
import org.ubiquia.core.flow.model.embeddable.FlowEventTimes;
import org.ubiquia.core.flow.model.entity.FlowEvent;


@Service
public class FlowEventDtoMapper extends GenericDtoMapper<FlowEvent, FlowEventDto> {

    private static final Logger logger = LoggerFactory.getLogger(FlowEventDtoMapper.class);

    @Override
    public FlowEventDto map(final FlowEvent from) throws JsonProcessingException {

        FlowEventDto to = null;
        if (Objects.nonNull(from)) {

            to = new FlowEventDto();
            super.setAbstractEntityFields(from, to);
            to.setBatchId(from.getBatchId());

            this.setFlowEventTimes(from, to);

            to.setHttpResponseCode(from.getHttpResponseCode());
            to.setId(from.getId());

            // Attempt to map the raw string representation in the database to an object
            // towards valid JSON.
            if (Objects.nonNull(from.getInputPayload())) {
                try {
                    to.setInputPayload(super.objectMapper.readValue(
                        from.getInputPayload(),
                        Object.class));
                } catch (JsonProcessingException e) {
                    logger.debug("Could not parse string into object; assuming raw string...");
                    to.setInputPayload(from.getInputPayload());
                }
            }

            // Attempt to map the raw string representation in the database to an object
            // towards valid JSON.
            if (Objects.nonNull(from.getOutputPayload())) {
                try {
                    to.setOutputPayload(super.objectMapper.readValue(
                        from.getOutputPayload(),
                        Object.class));
                } catch (JsonProcessingException e) {
                    logger.debug("Could not parse string into object; assuming raw string...");
                    to.setOutputPayload(from.getOutputPayload());
                }
            }

            to.setInputPayloadStamps(new ArrayList<>());
            for (var stamp : from.getInputPayloadStamps()) {
                var kvp = new KeyValuePairDto();
                kvp.setKey(stamp.getKey());
                // Attempt to parse the string into an object to avoid sending raw-string JSON
                try {
                    kvp.setValue(super.objectMapper.readValue(stamp.getValue(), Object.class));
                } catch (Exception e) {
                    kvp.setValue(stamp.getValue());
                }
                to.getInputPayloadStamps().add(kvp);
            }

            to.setOutputPayloadStamps(new ArrayList<>());
            for (var stamp : from.getOutputPayloadStamps()) {
                var kvp = new KeyValuePairDto();
                kvp.setKey(stamp.getKey());

                // Attempt to parse the string into an object to avoid sending raw-string JSON
                try {
                    kvp.setValue(super.objectMapper.readValue(
                        stamp.getValue(),
                        Object.class));
                } catch (Exception e) {
                    kvp.setValue(stamp.getValue());
                }
                to.getOutputPayloadStamps().add(kvp);
            }
            var adapter = new AdapterDto();
            adapter.setAdapterName(from.getAdapter().getAdapterName());
            adapter.setAdapterType(from.getAdapter().getAdapterType());

            to.setAdapter(adapter);
            to.setTags(from.getTags().stream().toList());
        }
        return to;
    }

    /**
     * Helper method to set a flow's event times.
     *
     * @param from The database entity.
     * @param to   The dto we're mapping to.
     */
    private void setFlowEventTimes(final FlowEvent from, FlowEventDto to) {
        var toFlowEventTimes = new FlowEventTimes();
        var fromFlowEventTimes = from.getFlowEventTimes();
        toFlowEventTimes.setAgentResponseTime(
            fromFlowEventTimes.getAgentResponseTime());
        toFlowEventTimes.setEgressResponseReceivedTime(
            fromFlowEventTimes.getEgressResponseReceivedTime());
        toFlowEventTimes.setPayloadEgressedTime(
            fromFlowEventTimes.getPayloadEgressedTime());
        toFlowEventTimes.setEventStartTime(
            fromFlowEventTimes.getEventStartTime());
        toFlowEventTimes.setEventCompleteTime(
            fromFlowEventTimes.getEventCompleteTime());
        toFlowEventTimes.setPollStartedTime(
            fromFlowEventTimes.getPollStartedTime());
        toFlowEventTimes.setSentToOutboxTime(
            fromFlowEventTimes.getSentToOutboxTime());
        to.setFlowEventTimes(toFlowEventTimes);
    }
}
