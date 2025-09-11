package org.ubiquia.core.communication.service.io.flow;

import java.net.URISyntaxException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.common.library.api.config.UbiquiaAgentConfig;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.core.communication.service.manager.flow.AdapterProxyManager;
import org.ubiquia.core.communication.service.manager.flow.ComponentProxyManager;

/**
 * Periodically polls the Flow Service for deployed graph IDs, synchronizes an in-memory
 * cache of {@link Graph} metadata, and notifies managers about deployments/teardowns.
 *
 * <p><strong>Overview</strong></p>
 * <ul>
 *   <li>Every {@code flowServiceConfig.pollFrequencyMilliseconds}, calls the Flow Service
 *       to retrieve all currently deployed graph IDs for the configured Ubiquia Agent.</li>
 *   <li>Keeps an internal map of known graphs (by ID) to detect new deployments and
 *       torn-down graphs between polling cycles.</li>
 *   <li>Fetches full {@link Graph} records for new deployments, updates the cache, and
 *       informs {@link AdapterProxyManager} and {@link ComponentProxyManager} to (un)register
 *       reverse-proxy routes or other resources.</li>
 * </ul>
 *
 * <p><strong>Threading</strong>: The default Spring scheduler runs the annotated method
 * on a single thread; the internal {@code currentGraphs} map is not synchronized.
 * If you change the scheduler to use a pool or invoke methods concurrently, consider
 * replacing it with a concurrent map or adding appropriate synchronization.</p>
 */
@Service
public class DeployedGraphPoller {

    private static final Logger logger = LoggerFactory.getLogger(DeployedGraphPoller.class);

    /** Cache of currently known deployed graphs, keyed by graph ID. */
    private final Map<String, Graph> currentGraphs = new HashMap<>();

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Autowired
    private AdapterProxyManager adapterProxyManager;

    @Autowired
    private ComponentProxyManager componentProxyManager;

    @Autowired
    private UbiquiaAgentConfig ubiquiaAgentConfig;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Scheduled polling task to keep track of deployed graph IDs and reconcile local state.
     *
     * <p>Pagination is handled by repeatedly calling {@link #fetchGraphIdsPage(Integer, Integer)}
     * until no additional pages are available. After collecting the full set of currently
     * deployed IDs, the method identifies new deployments and tear-downs and processes both.</p>
     *
     * <p>Schedule: {@code fixedRateString = "#{@flowServiceConfig.pollFrequencyMilliseconds}"}</p>
     */
    @Scheduled(fixedRateString = "#{@flowServiceConfig.pollFrequencyMilliseconds}")
    public void pollEndpoint() {
        logger.debug("Polling for deployed Graph IDs...");

        int page = 0;
        int size = 10;
        var currentlyDeployedGraphIds = new ArrayList<String>();

        try {
            boolean hasNextPage;
            do {
                var pageOfGraphIds = this.fetchGraphIdsPage(page, size);
                if (Objects.nonNull(pageOfGraphIds) && !pageOfGraphIds.getContent().isEmpty()) {
                    logger.debug("...polled Graph IDs from page {}: {}", page, pageOfGraphIds.getContent());
                    currentlyDeployedGraphIds.addAll(pageOfGraphIds.getContent());
                    hasNextPage = pageOfGraphIds.hasNext();
                    page++;
                } else {
                    logger.debug("...no more Graph IDs to poll.");
                    hasNextPage = false;
                }
            } while (hasNextPage);

            this.tryProcessDeployedGraphIds(currentlyDeployedGraphIds);
        } catch (Exception e) {
            logger.error("Error during polling: {}", e.getMessage(), e);
        }
    }

    /**
     * Reconciles the in-memory state with the latest set of deployed graph IDs by:
     * <ol>
     *   <li>Determining newly deployed and newly torn-down IDs,</li>
     *   <li>Fetching full {@link Graph} metadata for new deployments,</li>
     *   <li>Notifying managers about deployments and teardowns.</li>
     * </ol>
     *
     * @param currentlyDeployedGraphIds the complete list of IDs reported by the Flow Service
     * @throws URISyntaxException if building manager target URIs fails during processing
     */
    private void tryProcessDeployedGraphIds(final List<String> currentlyDeployedGraphIds)
        throws URISyntaxException {
        var newlyDeployedGraphIds = this.identifyNewlyDeployedGraphIds(currentlyDeployedGraphIds);
        var newlyTornDownGraphIds = this.identifyNewlyTornDownGraphIds(currentlyDeployedGraphIds);

        this.queryNewlyDeployedGraphs(newlyDeployedGraphIds);

        this.processNewDeployments(newlyDeployedGraphIds);
        this.processTornDownGraphs(newlyTornDownGraphIds);
    }

    /**
     * Computes the set difference: IDs present now but not in {@link #currentGraphs}.
     *
     * @param currentlyDeployedGraphIds IDs returned by the Flow Service
     * @return list of newly deployed graph IDs
     */
    private List<String> identifyNewlyDeployedGraphIds(List<String> currentlyDeployedGraphIds) {
        var newlyDeployed = new ArrayList<String>();
        for (var id : currentlyDeployedGraphIds) {
            if (!this.currentGraphs.containsKey(id)) {
                logger.info("...identified newly-deployed graph ID: {}", id);
                newlyDeployed.add(id);
            }
        }
        return newlyDeployed;
    }

    /**
     * Computes the set difference: IDs in {@link #currentGraphs} that are no longer reported.
     *
     * @param currentlyDeployedGraphIds IDs returned by the Flow Service
     * @return list of newly torn-down graph IDs
     */
    private List<String> identifyNewlyTornDownGraphIds(List<String> currentlyDeployedGraphIds) {
        var newlyTornDown = new ArrayList<String>();
        for (var id : this.currentGraphs.keySet()) {
            if (!currentlyDeployedGraphIds.contains(id)) {
                logger.info("...identified newly torn-down graph ID: {}", id);
                newlyTornDown.add(id);
            }
        }
        return newlyTornDown;
    }

    /**
     * Fetches and caches full {@link Graph} records for newly deployed IDs by querying the
     * Flow Service {@code /ubiquia/flow-service/graph/query/params} endpoint with {@code id}.
     *
     * @param newlyDeployedGraphIds IDs to resolve into {@link Graph} objects
     */
    private void queryNewlyDeployedGraphs(final List<String> newlyDeployedGraphIds) {
        for (var id : newlyDeployedGraphIds) {
            var targetUri = UriComponentsBuilder
                .fromHttpUrl(this.flowServiceConfig.getUrl() + ":" + this.flowServiceConfig.getPort())
                .path("/ubiquia/flow-service/graph/query/params")
                .queryParam("page", 0)
                .queryParam("size", 1)
                .queryParam("id", id)
                .buildAndExpand(this.ubiquiaAgentConfig.getId())
                .toUriString();

            var headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            var requestEntity = new HttpEntity<>(headers);
            var responseEntity = this.restTemplate.exchange(
                targetUri,
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<GenericPageImplementation<Graph>>() {}
            );

            if (responseEntity.getStatusCode().is2xxSuccessful()
                && Objects.nonNull(responseEntity.getBody())
                && !responseEntity.getBody().getContent().isEmpty()) {
                this.currentGraphs.put(id, responseEntity.getBody().getContent().get(0));
            } else {
                logger.warn("No data retrieved for Graph ID {}. Status: {}", id, responseEntity.getStatusCode());
            }
        }
    }

    /**
     * Notifies managers about newly deployed graphs and allows them to create/register
     * any necessary proxy routes or component wiring, then updates internal state as needed.
     *
     * @param newlyDeployedGraphIds list of graph IDs that were newly deployed
     * @throws URISyntaxException if a manager requires URI construction that fails
     */
    private void processNewDeployments(final List<String> newlyDeployedGraphIds)
        throws URISyntaxException {
        for (var id : newlyDeployedGraphIds) {
            var graph = this.currentGraphs.get(id);
            this.adapterProxyManager.tryProcessNewlyDeployedGraph(graph);
            this.componentProxyManager.tryProcessNewlyDeployedGraph(graph);
        }
    }

    /**
     * Notifies managers about torn-down graphs so they can remove proxy routes or other
     * resources, then evicts the graph from the internal cache.
     *
     * @param newlyTornDownGraphIds list of graph IDs that were removed
     */
    private void processTornDownGraphs(final List<String> newlyTornDownGraphIds) {
        for (var id : newlyTornDownGraphIds) {
            var graph = this.currentGraphs.get(id);
            this.adapterProxyManager.tryProcessNewlyTornDownGraph(graph);
            this.componentProxyManager.tryProcessNewlyTornDownGraph(graph);
            this.currentGraphs.remove(id);
        }
    }

    /**
     * Retrieves one page of deployed graph IDs for the configured Ubiquia Agent.
     *
     * <p>Calls the Flow Service endpoint
     * {@code /ubiquia/ubiquia-agent/{agentId}/get-deployed-graph-ids?page=&size=} and returns
     * the deserialized page model.</p>
     *
     * @param pageNumber zero-based page index
     * @param pageSize   page size
     * @return a page of graph IDs, or {@code null} on non-2xx responses
     */
    private GenericPageImplementation<String> fetchGraphIdsPage(
        final Integer pageNumber,
        final Integer pageSize) {

        var targetUri = UriComponentsBuilder
            .fromHttpUrl(this.flowServiceConfig.getUrl() + ":" + this.flowServiceConfig.getPort())
            .path("/ubiquia/flow-service/ubiquia-agent/{agentId}/get-deployed-graph-ids")
            .queryParam("page", pageNumber)
            .queryParam("size", pageSize)
            .buildAndExpand(this.ubiquiaAgentConfig.getId())
            .toUriString();

        var headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        var requestEntity = new HttpEntity<>(headers);
        var responseEntity = this.restTemplate.exchange(
            targetUri,
            HttpMethod.GET,
            requestEntity,
            new ParameterizedTypeReference<GenericPageImplementation<String>>() {}
        );

        if (responseEntity.getStatusCode().is2xxSuccessful()
            && Objects.nonNull(responseEntity.getBody())) {
            logger.debug("Received response: {}", responseEntity.getStatusCode());
            return responseEntity.getBody();
        } else {
            logger.warn("No data retrieved from page {}. Status: {}", pageNumber, responseEntity.getStatusCode());
            return null;
        }
    }
}
