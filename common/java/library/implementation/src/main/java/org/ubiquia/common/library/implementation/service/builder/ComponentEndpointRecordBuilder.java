package org.ubiquia.common.library.implementation.service.builder;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Component;

@Service
public class ComponentEndpointRecordBuilder {

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

    public String getBasePathFor(final String graphName, final String componentName) {
        return "/graph/"
            + graphName.toLowerCase()
            + "/component/"
            + componentName.toLowerCase();
    }
}