package org.ubiquia.core.communication.controller.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.dto.UbiquiaAgent;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.core.communication.interfaces.InterfaceUbiquiaDaoControllerProxy;
import reactor.core.publisher.Mono;

/**
 * Proxy controller for Flow Service {@code ubique- agent} endpoints.
 *
 * <p>
 * Mounts under {@code /ubiquia/communication-service/flow-service/ubiquia-agent}
 * and forwards requests to the downstream Flow Service base
 * {@code /ubiquia/flow-service/ubiquia-agent}. Uses reactive, non-blocking I/O
 * via {@link WebClient}. On downstream failures, emits a {@code 502 Bad Gateway}
 * with a null/empty body as noted per method.
 * </p>
 *
 * <p>
 * The downstream host/port are sourced from {@link FlowServiceConfig}; the fully
 * qualified base URL is provided by {@link #getUrlHelper()}.
 * </p>
 */
@RestController
@RequestMapping("/ubiquia/core/communication-service/flow-service/agent")
public class AgentControllerProxy implements InterfaceUbiquiaDaoControllerProxy {

    /** Flow Service host/port configuration used to build the downstream base URL. */
    @Autowired
    private FlowServiceConfig flowServiceConfig;

    /** Reactive HTTP client used to forward requests to the downstream service. */
    @Autowired
    private WebClient webClient;

    /**
     * Retrieves the singleton {@link UbiquiaAgent} instance from the Flow Service.
     *
     * <p>Proxies a {@code GET} to the downstream {@code /instance/get} endpoint and returns
     * the response as-is.</p>
     *
     * <p><strong>Error handling:</strong> On downstream error, returns {@code 502 Bad Gateway}
     * with a {@code null} body.</p>
     *
     * @return a {@link Mono} emitting the downstream {@link ResponseEntity} containing a
     *         {@link UbiquiaAgent}, or a {@code 502} response on error
     */
    @GetMapping("/instance/get")
    public Mono<ResponseEntity<UbiquiaAgent>> proxyGetInstance() {

        var targetUri = UriComponentsBuilder
            .fromHttpUrl(this.getUrlHelper() + "/instance/get")
            .build()
            .toUriString();

        return this.webClient
            .get()
            .uri(targetUri)
            .retrieve()
            .toEntity(UbiquiaAgent.class)
            .onErrorResume(e ->
                Mono.just(ResponseEntity.status(502).body(null))
            );
    }

    /**
     * Retrieves a paginated list of deployed graph IDs for a given agent.
     *
     * <p>Proxies a {@code GET} to the downstream
     * {@code /{id}/get-deployed-graph-ids?page=&size=} endpoint and deserializes the
     * result into {@code GenericPageImplementation&lt;String&gt;}.</p>
     *
     * <p><strong>Error handling:</strong> On downstream error, returns {@code 502 Bad Gateway}
     * with an empty {@link GenericPageImplementation} instance.</p>
     *
     * @param agentId the agent identifier
     * @param page    zero-based page index (default {@code 0})
     * @param size    page size (default {@code 10})
     * @return a {@link Mono} emitting the downstream {@link ResponseEntity} containing a paged list
     *         of graph IDs, or a {@code 502} response with an empty page on error
     */
    @GetMapping("/{id}/get-deployed-graph-ids")
    public Mono<ResponseEntity<GenericPageImplementation<String>>> proxyGetDeployedGraphIds(
        @PathVariable("id") String agentId,
        @RequestParam(value = "page", defaultValue = "0") Integer page,
        @RequestParam(value = "size", defaultValue = "10") Integer size) {

        var targetUri = UriComponentsBuilder
            .fromHttpUrl(this.getUrlHelper() + "/" + agentId + "/get-deployed-graph-ids")
            .queryParam("page", page)
            .queryParam("size", size)
            .build()
            .toUriString();

        return this.webClient
            .get()
            .uri(targetUri)
            .retrieve()
            .toEntity(new ParameterizedTypeReference<GenericPageImplementation<String>>() {})
            .onErrorResume(e ->
                Mono.just(ResponseEntity.status(502).body(new GenericPageImplementation<>()))
            );
    }

    /**
     * Builds the base URL for the downstream Flow Service {@code ubique- agent} endpoints.
     *
     * <p>Example format:
     * {@code http://<host>:<port>/ubiquia/flow-service/ubiquia-agent}</p>
     *
     * @return fully qualified base URL used by proxy methods
     */
    @Override
    public String getUrlHelper() {
        var url = this.flowServiceConfig.getUrl()
            + ":"
            + this.flowServiceConfig.getPort().toString()
            + "/ubiquia/core/flow-service/agent";
        return url;
    }
}
