package org.ubiquia.core.communication.service.manager.flow;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.ubiquia.common.library.implementation.service.builder.ComponentEndpointRecordBuilder;
import org.ubiquia.common.model.ubiquia.dto.Graph;

@Service
public class ComponentProxyManager {
    private static final Logger logger = LoggerFactory.getLogger(ComponentProxyManager.class);

    private final HashMap<String, URI> proxiedComponentEndpoints = new HashMap<>();

    @Autowired
    private ComponentEndpointRecordBuilder componentEndpointRecordBuilder;

    @Autowired
    private WebClient webClient;

    public void tryProcessNewlyDeployedGraph(final Graph graph) throws URISyntaxException {
        var componentsToProxy = graph
            .getComponents()
            .stream()
            .filter(component ->
                Boolean.TRUE.equals(
                    component.getCommunicationServiceSettings().getExposeViaCommService()))
            .toList();

        for (var component : componentsToProxy) {

            var componentName = component.getName().toLowerCase();
            if (!this.proxiedComponentEndpoints.containsKey(componentName)) {
                logger.info("Registering proxy for component: {}", component.getName());
                var endpoint = this
                    .componentEndpointRecordBuilder
                    .getComponentUriFrom(graph.getName(), component);
                logger.info("...proxying base url: {}", endpoint);
                this.proxiedComponentEndpoints.put(
                    componentName,
                    endpoint);
            }
        }
    }

    public void tryProcessNewlyTornDownGraph(final Graph graph) {
        var componentsToUnproxy = graph.getComponents().stream()
            .filter(component -> Boolean.TRUE.equals(
                component.getCommunicationServiceSettings().getExposeViaCommService()))
            .toList();

        for (var component : componentsToUnproxy) {

            if (this.proxiedComponentEndpoints.containsKey(component.getName())) {
                logger.info("...unproxying endpoints for component {}...",
                    component.getName());
                this.proxiedComponentEndpoints.remove(component.getName());
            }
        }
    }

    public List<String> getRegisteredEndpoints() {
        var endpoints = new ArrayList<String>();
        for (var uri : this.proxiedComponentEndpoints.values()) {
            endpoints.add(uri.getRawPath());
        }
        return endpoints;
    }

    public URI getRegisteredEndpointFor(final String componentName) {
        URI endpoint = null;
        if (this.proxiedComponentEndpoints.containsKey(componentName)) {
            endpoint = this.proxiedComponentEndpoints.get(componentName);
        }
        return endpoint;
    }
}
