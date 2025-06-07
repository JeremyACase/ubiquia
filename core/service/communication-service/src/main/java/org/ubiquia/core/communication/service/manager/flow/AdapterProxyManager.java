package org.ubiquia.core.communication.service.manager.flow;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.client.WebClient;
import org.ubiquia.common.model.ubiquia.dto.GraphDto;
import org.ubiquia.core.communication.config.FlowServiceConfig;
import org.ubiquia.core.communication.service.factory.AdapterProxiedEndpointFactory;
import reactor.core.publisher.Mono;

@Service
public class AdapterProxyManager {

    private static final Logger logger = LoggerFactory.getLogger(AdapterProxyManager.class);

    private final HashMap<String, ArrayList<URI>> proxiedAdapterMap = new HashMap<>();

    @Autowired
    private AdapterProxiedEndpointFactory adapterProxiedEndpointFactory;

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Autowired
    private WebClient webClient;

    /**
     * Proxies every endpoint for each adapter that needs to be proxied.
     */
    public void tryProcessNewlyDeployedGraph(final GraphDto graph) {
        var adaptersToProxy = graph.getAdapters().stream()
            .filter(adapter -> Boolean.TRUE.equals(
                adapter.getCommunicationServiceSettings().getExposeViaCommService()))
            .toList();

        for (var adapter : adaptersToProxy) {
            logger.info("...Proxying new endpoints for graph {} and adapter: {}...",
                graph.getGraphName(),
                adapter.getAdapterName());
            this.proxiedAdapterMap.put(adapter.getId(), new ArrayList<>());
            var endpointRecords = this.adapterProxiedEndpointFactory.getProxiedEndpointsFrom(
                adapter,
                graph);

            for (var endpointRecord : endpointRecords) {
                try {
                    var path = this.flowServiceConfig.getUrl()
                        + ":"
                        + this.flowServiceConfig.getPort()
                        + "/"
                        + endpointRecord.path();
                    var targetUri = new URI(path);

                    logger.info("...proxying to endpoint: {} [{}]",
                        targetUri,
                        endpointRecord.method());

                    // Convert RequestMethod to HttpMethod
                    var httpMethod = convertRequestMethod(endpointRecord.method());
                    if (Objects.isNull(httpMethod)) {
                        throw new RuntimeException("ERROR: Unsupported RequestMethod: "
                            + endpointRecord.method());
                    }

                    this.proxyEndpoint(targetUri, httpMethod)
                        .subscribe(response -> {
                            if (response.getStatusCode().is2xxSuccessful()) {
                                logger.info("...successfully proxied to {}", targetUri);
                                this.proxiedAdapterMap.get(adapter.getId()).add(targetUri);
                            } else {
                                logger.warn("WARNING: Failed to proxy to {}. Status: {}",
                                    targetUri,
                                    response.getStatusCode());
                            }
                        }, error -> {
                            logger.error("ERROR proxying to {}: {}",
                                targetUri,
                                error.getMessage(),
                                error);
                        });

                } catch (Exception e) {
                    logger.error("Error constructing URI for adapter: {}", adapter.getAdapterName(), e);
                }
            }
            logger.info("...done setting up proxy endpoints for adapter {}...",
                adapter.getAdapterName());
        }
        logger.info("...done setting up adapter proxies for graph {}...", graph.getGraphName());
    }

    /**
     * Uses WebClient to send a proxy request to the given target URI with the specified method.
     */
    private Mono<ResponseEntity<String>> proxyEndpoint(URI targetUri, HttpMethod method) {
        var requestSpec = this.webClient.method(method).uri(targetUri);
        return requestSpec
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(String.class);
    }

    /**
     * Converts Spring's RequestMethod to WebClient's HttpMethod.
     */
    private HttpMethod convertRequestMethod(RequestMethod requestMethod) {
        return switch (requestMethod) {
            case GET -> HttpMethod.GET;
            case POST -> HttpMethod.POST;
            case PUT -> HttpMethod.PUT;
            case DELETE -> HttpMethod.DELETE;
            case PATCH -> HttpMethod.PATCH;
            case HEAD -> HttpMethod.HEAD;
            case OPTIONS -> HttpMethod.OPTIONS;
            case TRACE -> HttpMethod.TRACE;
            default -> null;
        };
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

            if (this.proxiedAdapterMap.containsKey(adapter.getId())) {
                logger.info("...unproxying endpoints for adapter {}...",
                    adapter.getAdapterName());
                this.proxiedAdapterMap.remove(adapter.getId());
            }
        }
    }
}
