package org.ubiquia.common.library.implementation.service.builder;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Component;

/** Service for constructing endpoint URIs for component services within a graph. */
@Service
public class ComponentEndpointRecordBuilder {

    /**
     * Build a URI for a component's proxied endpoint within a given graph.
     *
     * @param graphName The name of the graph the component belongs to.
     * @param component The component whose endpoint URI is needed.
     * @return A fully formed {@link URI} targeting the component's proxied endpoint.
     * @throws URISyntaxException If the constructed URI string is malformed.
     */
    public URI getComponentUriFrom(final String graphName, final Component component)
        throws URISyntaxException {

        if (Objects.isNull(component.getCommunicationServiceSettings())) {
            throw new IllegalArgumentException("ERROR: Cannot get a component URI from component "
                + "without comm service settings...");
        }

        var endpoint = this.getBasePathFor(graphName, component.getName())
            + component.getCommunicationServiceSettings().getProxiedEndpoint();

        return new URI(
            "http",
            null,
            component.getName().toLowerCase(),
            component.getPort(),
            endpoint,
            null,
            null
        );
    }

    /**
     * Build the base URL path for a component within a graph.
     *
     * @param graphName     The name of the graph.
     * @param componentName The name of the component.
     * @return The base path string.
     */
    public String getBasePathFor(final String graphName, final String componentName) {
        return "/ubiquia/core-flow-service/"
            + graphName.toLowerCase()
            + "/component/"
            + componentName.toLowerCase();
    }
}