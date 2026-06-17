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
import org.ubiquia.common.library.implementation.service.builder.ComponentEndpointRecordBuilder;
import org.ubiquia.common.model.ubiquia.dto.Graph;

/**
 * Maintains an in-memory registry of component name → upstream URI for reverse-proxy routing.
 *
 * <p>Driven by graph deployment/teardown lifecycle events from {@link
 * org.ubiquia.core.communication.service.io.flow.DeployedGraphPoller}. Only components
 * with {@code communicationServiceSettings.exposeViaCommService == true} are registered.</p>
 */
@Service
public class ComponentProxyManager {

    private static final Logger logger = LoggerFactory.getLogger(ComponentProxyManager.class);

    private final HashMap<String, URI> proxiedComponentEndpoints = new HashMap<>();

    @Autowired
    private ComponentEndpointRecordBuilder componentEndpointRecordBuilder;

    /**
     * Registers proxy endpoints for components in a newly deployed graph that opt in to
     * exposure via the communication service.
     *
     * @param graph the deployed graph
     * @throws URISyntaxException if building a component endpoint URI fails
     */
    public void tryProcessNewlyDeployedGraph(final Graph graph) throws URISyntaxException {
        var componentsToProxy = graph
            .getComponents()
            .stream()
            .filter(component ->
                Boolean.TRUE.equals(
                    component.getCommunicationServiceSettings().getExposeViaCommService()))
            .toList();

        for (var component : componentsToProxy) {
            if (!this.proxiedComponentEndpoints.containsKey(component.getName())) {
                logger.info("Registering proxy for component: {}", component.getName());
                var endpoint = this.componentEndpointRecordBuilder
                    .getComponentUriFrom(graph.getName(), component);
                logger.info("...proxying base url: {}", endpoint);
                this.proxiedComponentEndpoints.put(component.getName(), endpoint);
            }
        }
    }

    /**
     * Unregisters proxy endpoints for all components in a torn-down graph.
     *
     * @param graph the torn-down graph
     */
    public void tryProcessNewlyTornDownGraph(final Graph graph) {
        logger.info("...processing torn down graph: {}", graph.getName());

        for (var component : graph.getComponents()) {
            logger.info("...checking if component {} needs to be torn down...",
                component.getName());
            if (this.proxiedComponentEndpoints.containsKey(component.getName())) {
                logger.info("...unproxying endpoints for component {}...", component.getName());
                this.proxiedComponentEndpoints.remove(component.getName());
            }
        }
    }

    /** Returns the raw paths of all currently registered upstream component endpoints. */
    public List<String> getRegisteredEndpoints() {
        var endpoints = new ArrayList<String>();
        for (var uri : this.proxiedComponentEndpoints.values()) {
            endpoints.add(uri.getRawPath());
        }
        return endpoints;
    }

    /**
     * Returns the upstream base URI for the given component name,
     * or {@code null} if not registered.
     *
     * @param componentName the component's logical name
     */
    public URI getRegisteredEndpointFor(final String componentName) {
        return this.proxiedComponentEndpoints.get(componentName);
    }
}
