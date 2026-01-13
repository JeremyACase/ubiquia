package org.ubiquia.core.flow.interfaces;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.ubiquia.core.flow.component.node.AbstractNode;

/**
 * An interface defining methods that can be used to egress data over brokers.
 */
public interface InterfaceBrokerEgress {

    /**
     * Attempt to publish the provided payload over any configured broker.
     *
     * @param payload The payload to publish.
     * @param adapter The adapter doing the publishing.
     * @throws JsonProcessingException Exceptions from JSON.
     */
    void tryPublishPayload(final String payload, final AbstractNode adapter)
        throws JsonProcessingException;
}
