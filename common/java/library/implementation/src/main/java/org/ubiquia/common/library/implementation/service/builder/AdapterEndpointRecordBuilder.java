package org.ubiquia.common.library.implementation.service.builder;


import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.record.EndpointRecord;

@Service
public class AdapterEndpointRecordBuilder {

    public URI getComponentUriFrom(final Node node)
        throws URISyntaxException {

        return new URI(
            "http",
            null,
            node.getComponent().getName().toLowerCase(),
            node.getComponent().getPort(),
            node.getEndpoint(),
            null,
            null
        );
    }

    public EndpointRecord getBackpressureEndpointFor(final String graphName, final String adapterName) {
        var path = this.getBasePathFor(graphName, adapterName) + "/back-pressure";
        return new EndpointRecord(path, RequestMethod.GET);
    }

    public EndpointRecord getPushEndpointFor(final String graphName, final String adapterName) {
        var path = this.getBasePathFor(graphName, adapterName) + "/push";
        return new EndpointRecord(path, RequestMethod.POST);
    }

    public EndpointRecord getPeekEndpointFor(final String graphName, final String adapterName) {
        var path = this.getBasePathFor(graphName, adapterName) + "/queue/peek";
        return new EndpointRecord(path, RequestMethod.GET);
    }

    public EndpointRecord getPopEndpointFor(final String graphName, final String adapterName) {
        var path = this.getBasePathFor(graphName, adapterName) + "/queue/pop";
        return new EndpointRecord(path, RequestMethod.GET);
    }

    public String getBasePathFor(final String graphName, final String adapterName) {
        return "graph/"
            + graphName.toLowerCase()
            + "/adapter/"
            + adapterName.toLowerCase();
    }
}