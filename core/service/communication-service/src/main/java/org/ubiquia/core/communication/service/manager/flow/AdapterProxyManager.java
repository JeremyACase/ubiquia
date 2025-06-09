package org.ubiquia.core.communication.service.manager.flow;

import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.ubiquia.common.library.logic.service.builder.AdapterEndpointRecordBuilder;
import org.ubiquia.common.model.ubiquia.dto.GraphDto;
import org.ubiquia.core.communication.config.FlowServiceConfig;

@Service
public class AdapterProxyManager {

    private static final Logger logger = LoggerFactory.getLogger(AdapterProxyManager.class);

    private final HashMap<String, String> proxiedAdapterEndpoints = new HashMap<>();

    @Autowired
    private AdapterEndpointRecordBuilder adapterEndpointRecordBuilder;

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Autowired
    private WebClient webClient;

    public void tryProcessNewlyDeployedGraph(final GraphDto graph) {
        var adaptersToProxy = graph.getAdapters().stream()
            .filter(adapter ->
                Boolean.TRUE.equals(
                    adapter.getCommunicationServiceSettings().getExposeViaCommService()))
            .toList();

        for (var adapter : adaptersToProxy) {

            if (!this.proxiedAdapterEndpoints.containsKey(adapter.getAdapterName())) {
                logger.info("Registering proxy for adapter: {}", adapter.getAdapterName());
                var endpoint = this.adapterEndpointRecordBuilder.getBasePathFor(
                    graph.getGraphName(),
                    adapter.getAdapterName());
                logger.info("...proxying base url: {}", endpoint);
                this.proxiedAdapterEndpoints.put(adapter.getAdapterName(), endpoint);
            }
        }
    }

    /**
     * Unproxies any previously proxied URIs for the torn-down graph.
     */
    public void tryProcessNewlyTornDownGraph(final GraphDto graph) {
        var adaptersToUnproxy = graph.getAdapters().stream()
            .filter(adapter -> Boolean.TRUE.equals(
                adapter.getCommunicationServiceSettings().getExposeViaCommService()))
            .toList();

        for (var adapter : adaptersToUnproxy) {

            if (this.proxiedAdapterEndpoints.containsKey(adapter.getAdapterName())) {
                logger.info("...unproxying endpoints for adapter {}...",
                    adapter.getAdapterName());
                this.proxiedAdapterEndpoints.remove(adapter.getAdapterName());
            }
        }
    }

    public List<String> getRegisteredEndpoints() {
        return this.proxiedAdapterEndpoints.values().stream().toList();
    }

    public String getRegisteredEndpointFor(final String adapterName) {
        String endpoint = null;
        if (this.proxiedAdapterEndpoints.containsKey(adapterName)) {
            endpoint = this.proxiedAdapterEndpoints.get(adapterName);
        }
        return endpoint;
    }
}
