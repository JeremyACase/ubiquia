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

/**
 * Manages reverse-proxy registrations for component endpoints derived from deployed {@link Graph}s.
 *
 * <p>
 * This service maintains an in-memory registry mapping component names to their upstream
 * {@link URI} base endpoints. It is typically invoked by a deployment poller or other lifecycle
 * orchestrator to register/unregister proxy routes for components that opt into exposure via the
 * communication service (i.e., {@code communicationServiceSettings.exposeViaCommService == true}).
 * </p>
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>On new graph deployments, registers the upstream endpoint for each eligible component.</li>
 *   <li>On graph teardown, removes previously registered component endpoints.</li>
 *   <li>Provides read APIs to list all registered paths or fetch one by component name.</li>
 * </ul>
 *
 * <p><strong>Thread-safety:</strong> Uses a plain {@link HashMap}; if accessed from multiple threads,
 * add synchronization or replace with a concurrent map.</p>
 *
 * <p><strong>Name casing:</strong> Keys are stored using {@code component.getName().toLowerCase()} on registration.
 * Callers to {@link #getRegisteredEndpointFor(String)} should normalize casing accordingly to avoid misses.</p>
 */
@Service
public class ComponentProxyManager {
    private static final Logger logger = LoggerFactory.getLogger(ComponentProxyManager.class);

    /** Registry of component name (lower-cased) → upstream component base {@link URI}. */
    private final HashMap<String, URI> proxiedComponentEndpoints = new HashMap<>();

    @Autowired
    private ComponentEndpointRecordBuilder componentEndpointRecordBuilder;

    /** Reserved for future health checks or metadata calls. */
    @Autowired
    private WebClient webClient;

    /**
     * Processes a newly deployed {@link Graph} by registering proxies for any components
     * that should be exposed via the communication service.
     *
     * <p>
     * For each component with {@code exposeViaCommService == true}, computes the upstream
     * base {@link URI} using {@link ComponentEndpointRecordBuilder#getComponentUriFrom(String, org.ubiquia.common.model.ubiquia.dto.Component)}
     * and stores it in the registry if not already present.
     * </p>
     *
     * @param graph the deployed graph whose components may need proxy registration
     * @throws URISyntaxException if building the component endpoint {@link URI} fails
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

    /**
     * Unregisters any previously proxied component endpoints associated with a torn-down graph.
     *
     * <p>
     * For each component with {@code exposeViaCommService == true}, removes its entry from the
     * registry if present.
     * </p>
     *
     * @param graph the torn-down graph whose components should be unproxied
     */
    public void tryProcessNewlyTornDownGraph(final Graph graph) {
        var componentsToUnproxy = graph
            .getComponents()
            .stream()
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

    /**
     * Returns the list of currently registered upstream paths (raw path portion of each {@link URI}).
     *
     * @return immutable snapshot list of registered endpoint paths (e.g., {@code /graph-x/component-y})
     */
    public List<String> getRegisteredEndpoints() {
        var endpoints = new ArrayList<String>();
        for (var uri : this.proxiedComponentEndpoints.values()) {
            endpoints.add(uri.getRawPath());
        }
        return endpoints;
    }

    /**
     * Looks up the upstream base {@link URI} for a given component name.
     *
     * <p><strong>Casing:</strong> Keys are stored lower-cased; normalize the input name
     * (e.g., {@code toLowerCase(Locale.ROOT)}) to avoid misses.</p>
     *
     * @param componentName the component's logical name
     * @return the registered upstream base {@link URI}, or {@code null} if not registered
     */
    public URI getRegisteredEndpointFor(final String componentName) {
        URI endpoint = null;
        if (this.proxiedComponentEndpoints.containsKey(componentName)) {
            endpoint = this.proxiedComponentEndpoints.get(componentName);
        }
        return endpoint;
    }
}
