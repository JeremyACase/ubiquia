package org.ubiquia.common.library.implementation.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.dto.FlowEvent;
import org.ubiquia.common.model.ubiquia.dto.KeyValuePair;
import org.ubiquia.common.model.ubiquia.embeddable.FlowEventTimes;
import org.ubiquia.common.model.ubiquia.entity.FlowEventEntity;


@Service
public class FlowEventDtoMapper extends GenericDtoMapper<FlowEventEntity, FlowEvent> {

    private static final Logger logger = LoggerFactory.getLogger(FlowEventDtoMapper.class);

    @Autowired
    private FlowDtoMapper flowDtoMapper;

    @Override
    public FlowEvent map(final FlowEventEntity from) throws JsonProcessingException {

        FlowEvent to = null;
        if (Objects.nonNull(from)) {

            to = new FlowEvent();
            super.setAbstractEntityFields(from, to);
            to.setFlow(this.flowDtoMapper.map(from.getFlow()));

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
                var kvp = new KeyValuePair();
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
                var kvp = new KeyValuePair();
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
            var adapter = new Node();
            adapter.setName(from.getNode().getName());
            adapter.setNodeType(from.getNode().getNodeType());

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
    private void setFlowEventTimes(final FlowEventEntity from, FlowEvent to) {
        var toFlowEventTimes = new FlowEventTimes();
        var fromFlowEventTimes = from.getFlowEventTimes();
        toFlowEventTimes.setComponentResponseTime(
            fromFlowEventTimes.getComponentResponseTime());
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
