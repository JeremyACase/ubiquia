package org.ubiquia.core.communication.service.manager.flow;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.ubiquia.common.library.logic.service.builder.AdapterUriBuilder;
import org.ubiquia.common.model.ubiquia.dto.GraphDto;
import reactor.core.publisher.Mono;

@Service
public class AdapterProxyManager {

    private static final Logger logger = LoggerFactory.getLogger(AdapterProxyManager.class);
    private final Set<URI> proxiedAdapterUris = ConcurrentHashMap.newKeySet();
    @Autowired
    private AdapterUriBuilder adapterUriBuilder;
    @Autowired
    private WebClient webClient;

    public void tryProcessNewlyDeployedGraph(final GraphDto graph) {
        var adaptersToProxy = graph.getAdapters().stream()
            .filter(x -> Boolean.TRUE.equals(
                x.getCommunicationServiceSettings().getExposeViaCommService()))
            .toList();

        for (var adapter : adaptersToProxy) {
            try {
                var targetUri = this.adapterUriBuilder.getAgentUriFrom(adapter);
                logger.info("...proxying requests to adapter URI: {}", targetUri);

                this.proxyAdapterUri(targetUri)
                    .subscribe(responseEntity -> {
                        if (responseEntity.getStatusCode().is2xxSuccessful()) {
                            logger.info("Adapter URI {} responded with: {}",
                                targetUri,
                                responseEntity.getBody());
                            this.proxiedAdapterUris.add(targetUri);
                        } else {
                            logger.warn("Adapter URI {} responded with status: {}",
                                targetUri,
                                responseEntity.getStatusCode());
                        }
                    }, error -> {
                        logger.error("ERROR while proxying adapter URI {}: {}",
                            targetUri,
                            error.getMessage(),
                            error);
                    });

            } catch (Exception e) {
                logger.error("ERROR building adapter URI: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Unproxy any adapters that were previously proxied for the torn-down graph.
     */
    public void tryProcessNewlyTornDownGraph(final GraphDto graph) {
        var adaptersToUnproxy = graph.getAdapters().stream()
            .filter(x -> Boolean.TRUE.equals(
                x.getCommunicationServiceSettings().getExposeViaCommService()))
            .toList();

        for (var adapter : adaptersToUnproxy) {
            try {
                var targetUri = this.adapterUriBuilder.getAgentUriFrom(adapter);
                if (this.proxiedAdapterUris.contains(targetUri)) {
                    this.proxiedAdapterUris.remove(targetUri);
                    logger.info("Unproxying adapter URI: {}", targetUri);
                }
            } catch (Exception e) {
                logger.error("Error while unproxying adapter: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Helper method to proxy the adapter URI using WebClient.
     */
    private Mono<ResponseEntity<String>> proxyAdapterUri(final URI targetUri) {
        return this.webClient.get()
            .uri(targetUri)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(String.class);
    }
}
