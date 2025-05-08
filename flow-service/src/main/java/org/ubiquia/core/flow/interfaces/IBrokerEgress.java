package org.ubiquia.core.flow.interfaces;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.ubiquia.core.flow.component.adapter.AAdapter;

/**
 * An interface defining methods that can be used to egress data over brokers.
 */
public interface IBrokerEgress {

    /**
     * Attempt to publish the provided payload over any configured broker.
     *
     * @param payload The payload to publish.
     * @param adapter The adapter doing the publishing.
     * @throws JsonProcessingException Exceptions from JSON.
     */
    void tryPublishPayload(final String payload, final AAdapter adapter)
        throws JsonProcessingException;

}
