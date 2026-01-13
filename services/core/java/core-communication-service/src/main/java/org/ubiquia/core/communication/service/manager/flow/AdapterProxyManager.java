package org.ubiquia.core.communication.service.manager.flow;

import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.library.implementation.service.builder.NodeEndpointRecordBuilder;
import org.ubiquia.common.model.ubiquia.dto.Graph;

/**
 * Manages adapter reverse-proxy registrations derived from deployed {@link Graph}s.
 *
 * <p>
 * This service tracks which adapter endpoints should be exposed via the
 * communication service (based on each adapter's
 * {@code communicationServiceSettings.exposeViaCommService}) and maintains an
 * in-memory registry mapping adapter names to their proxied base paths. It is
 * typically driven by lifecycle notifications from a deployment poller.
 * </p>
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>On new graph deployments, registers proxy base paths for eligible adapters.</li>
 *   <li>On graph teardown, removes previously registered adapters for that graph.</li>
 *   <li>Provides read APIs to list all registered endpoints or fetch one by adapter name.</li>
 * </ul>
 *
 * <p><strong>Threading:</strong> Uses a plain {@link HashMap}; if accessed from multiple
 * threads, wrap with appropriate synchronization or replace with a concurrent map.</p>
 *
 * <p><strong>Note on name keys:</strong> Newly registered adapters are stored under a
 * lower-cased key (adapter name), while unregistration looks up using the adapter's
 * original casing. Ensure consistent casing when calling {@link #getRegisteredEndpointFor(String)}.
 * </p>
 */
@Service
public class AdapterProxyManager {

    private static final Logger logger = LoggerFactory.getLogger(AdapterProxyManager.class);

    /**
     * Registry of adapter name → proxied base endpoint path.
     */
    private final HashMap<String, String> proxiedAdapterEndpoints = new HashMap<>();

    @Autowired
    private NodeEndpointRecordBuilder nodeEndpointRecordBuilder;

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Autowired
    private WebClient webClient;

    /**
     * Processes a newly deployed {@link Graph} by registering proxies for any adapters
     * that should be exposed via the communication service.
     *
     * <p>
     * For each adapter with {@code exposeViaCommService == true}, computes the adapter's
     * proxied base path using {@link NodeEndpointRecordBuilder#getBasePathFor(String, String)}
     * and stores it in the in-memory registry if not already present.
     * </p>
     *
     * @param graph the deployed graph whose adapters may need proxy registration
     */
    public void tryProcessNewlyDeployedGraph(final Graph graph) {

        var adaptersToProxy = graph
            .getNodes()
            .stream()
            .filter(adapter ->
                Boolean.TRUE.equals(
                    adapter.getCommunicationServiceSettings().getExposeViaCommService()))
            .toList();

        for (var adapter : adaptersToProxy) {

            var adapterName = adapter.getName().toLowerCase();
            if (!this.proxiedAdapterEndpoints.containsKey(adapterName)) {
                logger.info("Registering proxy for adapter: {}", adapter.getName());
                var endpoint = this.nodeEndpointRecordBuilder.getBasePathFor(
                    graph.getName(),
                    adapterName);
                logger.info("...proxying base url: {}", endpoint);
                this.proxiedAdapterEndpoints.put(
                    adapterName,
                    endpoint);
            }
        }
    }

    /**
     * Unregisters any previously proxied adapter endpoints associated with a torn-down graph.
     *
     * <p>
     * For each adapter with {@code exposeViaCommService == true}, removes its entry
     * from the registry if present.
     * </p>
     *
     * @param graph the torn-down graph whose adapters should be unproxied
     */
    public void tryProcessNewlyTornDownGraph(final Graph graph) {
        var adaptersToUnproxy = graph.getNodes().stream()
            .filter(adapter -> Boolean.TRUE.equals(
                adapter.getCommunicationServiceSettings().getExposeViaCommService()))
            .toList();

        for (var adapter : adaptersToUnproxy) {

            if (this.proxiedAdapterEndpoints.containsKey(adapter.getName())) {
                logger.info("...unproxying endpoints for adapter {}...",
                    adapter.getName());
                this.proxiedAdapterEndpoints.remove(adapter.getName());
            }
        }
    }

    /**
     * Returns the list of all currently registered proxied adapter endpoints.
     *
     * @return immutable snapshot list of endpoint base paths
     */
    public List<String> getRegisteredEndpoints() {
        return this.proxiedAdapterEndpoints.values().stream().toList();
    }

    /**
     * Looks up the proxied base endpoint for a given adapter name.
     *
     * <p><strong>Casing:</strong> Keys may be stored in lower case; callers should
     * normalize their input (e.g., {@code toLowerCase(Locale.ROOT)}) to avoid misses.</p>
     *
     * @param adapterName the adapter's logical name
     * @return the registered base endpoint (e.g., {@code "graph-x/adapter-y"}), or {@code null} if not registered
     */
    public String getRegisteredEndpointFor(final String adapterName) {
        String endpoint = null;
        if (this.proxiedAdapterEndpoints.containsKey(adapterName)) {
            endpoint = this.proxiedAdapterEndpoints.get(adapterName);
        }
        return endpoint;
    }
}
