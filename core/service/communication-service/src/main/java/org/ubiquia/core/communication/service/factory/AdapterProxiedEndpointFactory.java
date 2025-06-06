package org.ubiquia.core.communication.service.factory;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.logic.service.builder.AdapterEndpointRecordBuilder;
import org.ubiquia.common.library.logic.service.builder.EndpointRecord;
import org.ubiquia.common.model.ubiquia.dto.AdapterDto;
import org.ubiquia.common.model.ubiquia.dto.GraphDto;

@Service
public class AdapterProxiedEndpointFactory {

    private static final Logger logger = LoggerFactory.getLogger(AdapterProxiedEndpointFactory.class);

    @Autowired
    private AdapterEndpointRecordBuilder adapterEndpointRecordBuilder;

    public List<EndpointRecord> getProxiedEndpointsFrom(
        final AdapterDto adapter,
        final GraphDto graph) {
        logger.debug("Building proxied endpoints for adapter type: {}", adapter.getAdapterType());

        var graphName = graph.getGraphName();
        var adapterName = adapter.getAdapterName();

        // Determine which endpoints to include for each adapter type
        var endpoints = switch (adapter.getAdapterType()) {
            case EGRESS -> List.of(
                adapterEndpointRecordBuilder.getPushEndpointFor(graphName, adapterName),
                adapterEndpointRecordBuilder.getBackpressureEndpointFor(graphName, adapterName)
            );
            case MERGE -> List.of(
                adapterEndpointRecordBuilder.getBackpressureEndpointFor(graphName, adapterName)
            );
            case POLL, PUSH, SUBSCRIBE -> List.of(
                adapterEndpointRecordBuilder.getPushEndpointFor(graphName, adapterName)
            );
            case QUEUE -> List.of(
                adapterEndpointRecordBuilder.getBackpressureEndpointFor(graphName, adapterName),
                adapterEndpointRecordBuilder.getPeekEndpointFor(graphName, adapterName),
                adapterEndpointRecordBuilder.getPopEndpointFor(graphName, adapterName)
            );
            case HIDDEN, PUBLISH -> List.of(
                adapterEndpointRecordBuilder.getBackpressureEndpointFor(graphName, adapterName),
                adapterEndpointRecordBuilder.getPushEndpointFor(graphName, adapterName)
            );
            default -> throw new IllegalArgumentException("ERROR: Unrecognized adapter type: " + adapter.getAdapterType());
        };

        logger.debug("...finished building proxied endpoints.");
        return endpoints;
    }
}
