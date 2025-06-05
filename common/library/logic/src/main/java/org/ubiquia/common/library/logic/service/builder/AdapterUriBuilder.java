package org.ubiquia.common.library.logic.service.builder;

import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.AdapterDto;

@Service
public class AdapterUriBuilder {

    /**
     * Get the adapter's agent's URI.
     *
     * @param adapter The adapter to get a URI endpoint from.
     * @return The URI of the endpoint.
     * @throws URISyntaxException Exception from creating a URI.
     */
    public URI getAgentUriFrom(final AdapterDto adapter)
        throws URISyntaxException {

        var uri = new URI("http",
            null,
            adapter.getAgent().getAgentName().toLowerCase(),
            adapter.getAgent().getPort(),
            adapter.getEndpoint(),
            null,
            null
        );
        return uri;
    }
}
