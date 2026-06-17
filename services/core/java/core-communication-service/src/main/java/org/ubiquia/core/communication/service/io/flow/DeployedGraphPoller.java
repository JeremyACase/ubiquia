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
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.core.communication.service.manager.flow.ComponentProxyManager;
import org.ubiquia.core.communication.service.manager.flow.NodeProxyManager;

/**
 * Periodically polls the Flow Service for deployed graph IDs, keeps an in-memory cache of
 * {@link Graph} metadata, and notifies managers about deployments and teardowns.
 */
@Service
public class DeployedGraphPoller {

    private static final Logger logger = LoggerFactory.getLogger(DeployedGraphPoller.class);

    private final Map<String, Graph> currentGraphs = new HashMap<>();

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Autowired
    private NodeProxyManager nodeProxyManager;

    @Autowired
    private ComponentProxyManager componentProxyManager;

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private RestTemplate restTemplate;

    /** Polls the Flow Service for currently deployed graph IDs and reconciles local state. */
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
                    logger.debug("...polled Graph IDs from page {}: {}",
                        page, pageOfGraphIds.getContent());
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

    private void tryProcessDeployedGraphIds(final List<String> currentlyDeployedGraphIds)
        throws URISyntaxException {
        var newlyDeployedGraphIds = this.identifyNewlyDeployedGraphIds(currentlyDeployedGraphIds);
        var newlyTornDownGraphIds = this.identifyNewlyTornDownGraphIds(currentlyDeployedGraphIds);

        this.queryNewlyDeployedGraphs(newlyDeployedGraphIds);

        this.processNewDeployments(newlyDeployedGraphIds);
        this.processTornDownGraphs(newlyTornDownGraphIds);
    }

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

    private void queryNewlyDeployedGraphs(final List<String> newlyDeployedGraphIds) {
        for (var id : newlyDeployedGraphIds) {
            var targetUri = UriComponentsBuilder
                .fromHttpUrl(this.flowServiceConfig.getBaseUrl())
                .path("/ubiquia/core/flow-service/graph/query/params")
                .queryParam("page", 0)
                .queryParam("size", 1)
                .queryParam("id", id)
                .buildAndExpand(this.agentConfig.getId())
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
                var graph = responseEntity.getBody().getContent().get(0);
                this.currentGraphs.put(id, graph);
            } else {
                logger.warn("No data retrieved for Graph ID {}. Status: {}",
                    id, responseEntity.getStatusCode());
            }
        }
    }

    private void processNewDeployments(final List<String> newlyDeployedGraphIds)
        throws URISyntaxException {
        for (var id : newlyDeployedGraphIds) {
            var graph = this.currentGraphs.get(id);
            this.nodeProxyManager.tryProcessNewlyDeployedGraph(graph);
            this.componentProxyManager.tryProcessNewlyDeployedGraph(graph);
        }
    }

    private void processTornDownGraphs(final List<String> newlyTornDownGraphIds) {
        for (var id : newlyTornDownGraphIds) {
            var graph = this.currentGraphs.get(id);
            this.nodeProxyManager.tryProcessNewlyTornDownGraph(graph);
            this.componentProxyManager.tryProcessNewlyTornDownGraph(graph);
            this.currentGraphs.remove(id);
        }
    }

    private GenericPageImplementation<String> fetchGraphIdsPage(
        final Integer pageNumber,
        final Integer pageSize) {

        var targetUri = UriComponentsBuilder
            .fromHttpUrl(this.flowServiceConfig.getBaseUrl())
            .path("/ubiquia/core/flow-service/agent/{agentId}/get-deployed-graph-ids")
            .queryParam("page", pageNumber)
            .queryParam("size", pageSize)
            .buildAndExpand(this.agentConfig.getId())
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
            logger.warn("No data retrieved from page {}. Status: {}",
                pageNumber, responseEntity.getStatusCode());
            return null;
        }
    }
}
