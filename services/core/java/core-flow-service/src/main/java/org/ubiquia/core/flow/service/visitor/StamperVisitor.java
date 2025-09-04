package org.ubiquia.core.flow.service.visitor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.embeddable.KeyValuePair;
import org.ubiquia.common.model.ubiquia.entity.FlowEventEntity;

/**
 * This is a service that can "stamp" payloads from adapters per their configuration. Stamps
 * are data structures that will be persisted in the database as key-value-pairs where the
 * key is the stamp "keychain" and the value is the "value" retrieved using that keychain.
 */
@Service
public class StamperVisitor {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Attempt to stamp an input payload for an adapter.
     *
     * @param flowEventEntity  The event associated with the payload.
     * @param inputPayload The payload to stamp.
     * @throws JsonProcessingException Exception from trying to retrieve the stamps.
     */
    public void tryStampInputs(FlowEventEntity flowEventEntity, final String inputPayload)
        throws JsonProcessingException {

        for (var stampKeychain : flowEventEntity
            .getAdapter()
            .getAdapterSettings()
            .getInputStampKeychains()) {

            var stamp = this.getStampFrom(inputPayload, stampKeychain);
            var kvp = new KeyValuePair();
            kvp.setKey(stampKeychain);
            kvp.setValue(stamp);
            flowEventEntity.getInputPayloadStamps().add(kvp);
        }
    }

    /**
     * Attempt to stamp an output payload for an adapter.
     *
     * @param flowEventEntity   The event associated with the payload.
     * @param outputPayload The payload to stamp.
     * @throws JsonProcessingException Exception from trying to retrieve the stamps.
     */
    public void tryStampOutputs(FlowEventEntity flowEventEntity, final String outputPayload)
        throws JsonProcessingException {

        for (var stampKeychain : flowEventEntity
            .getAdapter()
            .getAdapterSettings()
            .getOutputStampKeychains()) {
            var stamp = this.getStampFrom(outputPayload, stampKeychain);
            var kvp = new KeyValuePair();
            kvp.setKey(stampKeychain);
            kvp.setValue(stamp);
            flowEventEntity.getOutputPayloadStamps().add(kvp);
        }
    }

    /**
     * Get a stamp from a payload by using the keychain.
     *
     * @param payload  The payload to extract from.
     * @param keyChain The keychain defining what value to extract from the payload.
     * @return The extracted value, if found.
     * @throws JsonProcessingException Exception from being unable to parse the JSON.
     */
    private String getStampFrom(final String payload, final String keyChain)
        throws JsonProcessingException {

        String stamp = null;
        var splitKeys = keyChain.split("\\.");
        var node = this.objectMapper.readTree(payload);
        if (node.isArray()) {
            var stampList = new ArrayList<>();
            for (var elem : node) {
                for (int i = 0; i < splitKeys.length; i++) {
                    node = this.getChildNode(elem, splitKeys[i]);
                }

                if (node.isTextual()) {
                    stamp = node.textValue();
                } else if (node.isIntegralNumber()) {
                    stamp = String.valueOf(node.intValue());
                }
                stampList.add(stamp);
            }
            stamp = stampList.toString();
        } else {
            for (int i = 0; i < splitKeys.length; i++) {
                node = this.getChildNode(node, splitKeys[i]);
            }

            if (node.isTextual()) {
                stamp = node.textValue();
            } else if (node.isIntegralNumber()) {
                stamp = String.valueOf(node.intValue());
            }
        }
        return stamp;
    }

    /**
     * Helper method to return a child node given a child node's field name.
     *
     * @param node           The node we're finding a child from.
     * @param childFieldName The name of the child node to look for.
     * @return The child node.
     */
    private JsonNode getChildNode(final JsonNode node, final String childFieldName) {
        if (!node.has(childFieldName)) {
            throw new IllegalArgumentException("ERROR: Could not find "
                + "field for name: "
                + childFieldName);
        }
        var value = node.get(childFieldName);
        return value;
    }
}
