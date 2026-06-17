package org.ubiquia.core.communication.controller.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.dto.Agent;
import org.ubiquia.core.communication.interfaces.InterfaceUbiquiaDaoControllerProxy;
import reactor.core.publisher.Mono;

/**
 * Proxy controller for Flow Service agent endpoints.
 *
 * <p>Forwards requests to the downstream Flow Service agent resource.
 * Uses reactive, non-blocking I/O; emits {@code 502 Bad Gateway} on downstream failures.</p>
 */
@RestController
@RequestMapping("/ubiquia/core/communication-service/flow-service/agent")
public class AgentControllerProxy extends AbstractUbiquiaDaoControllerProxy<Agent>
    implements InterfaceUbiquiaDaoControllerProxy {

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    /** Proxies a request to retrieve the current agent instance. */
    @GetMapping("/instance/get")
    public Mono<ResponseEntity<Agent>> proxyGetInstance() {
        return this.webClient
            .get()
            .uri(this.getUrlHelper() + "/instance/get")
            .retrieve()
            .toEntity(Agent.class)
            .onErrorResume(e -> Mono.just(ResponseEntity.status(502).body(null)));
    }

    /** Proxies a paginated request for deployed graph IDs for the given agent. */
    @GetMapping("/{id}/get-deployed-graph-ids")
    public Mono<ResponseEntity<GenericPageImplementation<String>>> proxyGetDeployedGraphIds(
        @PathVariable("id") final String agentId,
        @RequestParam(value = "page", defaultValue = "0") final Integer page,
        @RequestParam(value = "size", defaultValue = "10") final Integer size) {

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
            .onErrorResume(
                e -> Mono.just(
                    ResponseEntity.status(502).body(new GenericPageImplementation<>())));
    }

    @Override
    public String getUrlHelper() {
        return this.flowServiceConfig.getBaseUrl() + "/ubiquia/core/flow-service/agent";
    }
}
