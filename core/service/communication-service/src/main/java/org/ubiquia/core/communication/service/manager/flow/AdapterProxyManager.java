package org.ubiquia.core.communication.service.manager.flow;

import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.ubiquia.common.model.ubiquia.dto.GraphDto;
import org.ubiquia.core.communication.config.FlowServiceConfig;
import org.ubiquia.core.communication.service.factory.AdapterProxiedEndpointFactory;

@Service
public class AdapterProxyManager {

    private static final Logger logger = LoggerFactory.getLogger(AdapterProxyManager.class);

    private final HashSet<String> registerAdapters = new HashSet<>();

    @Autowired
    private AdapterProxiedEndpointFactory adapterProxiedEndpointFactory;

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

            if (!this.registerAdapters.contains(adapter.getAdapterName())) {
                logger.info("Registering proxy for adapter: {}", adapter.getAdapterName());
                this.registerAdapters.add(adapter.getAdapterName().toLowerCase());
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

            if (this.registerAdapters.contains(adapter.getAdapterName())) {
                logger.info("...unproxying endpoints for adapter {}...",
                    adapter.getAdapterName());
                this.registerAdapters.remove(adapter.getAdapterName());
            }
        }
    }

    public HashSet<String> getRegisteredAdapters() {
        return registerAdapters;
    }
}
