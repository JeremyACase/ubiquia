package org.ubiquia.common.library.logic.service.builder;

import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.ubiquia.common.model.ubiquia.dto.AdapterDto;

@Service
public class AdapterEndpointRecordBuilder {

    public URI getAgentUriFrom(final AdapterDto adapter)
        throws URISyntaxException {

        return new URI(
            "http",
            null,
            adapter.getAgent().getAgentName().toLowerCase(),
            adapter.getAgent().getPort(),
            adapter.getEndpoint(),
            null,
            null
        );
    }

    public EndpointRecord getBackpressureEndpointFor(final String graphName, final String adapterName) {
        var path = this.getPathHelper(graphName, adapterName) + "/back-pressure";
        return new EndpointRecord(path, RequestMethod.GET);
    }

    public EndpointRecord getPushEndpointFor(final String graphName, final String adapterName) {
        var path = this.getPathHelper(graphName, adapterName) + "/push";
        return new EndpointRecord(path, RequestMethod.POST);
    }

    public EndpointRecord getPeekEndpointFor(final String graphName, final String adapterName) {
        var path = this.getPathHelper(graphName, adapterName) + "/queue/peek";
        return new EndpointRecord(path, RequestMethod.GET);
    }

    public EndpointRecord getPopEndpointFor(final String graphName, final String adapterName) {
        var path = this.getPathHelper(graphName, adapterName) + "/queue/pop";
        return new EndpointRecord(path, RequestMethod.GET);
    }

    public String getPathHelper(final String graphName, final String adapterName) {
        return "ubiquia/graph/"
            + graphName.toLowerCase()
            + "/adapter/"
            + adapterName.toLowerCase();
    }
}
