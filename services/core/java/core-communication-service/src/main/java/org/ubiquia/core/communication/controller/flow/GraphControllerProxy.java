package org.ubiquia.core.communication.controller.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.ubiquia.IngressResponse;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.model.ubiquia.embeddable.BeliefStateGeneration;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import reactor.core.publisher.Mono;

/**
 * Proxy controller for Flow Service graph endpoints.
 *
 * <p>
 * Exposes a communication-service facade under
 * {@code /ubiquia/communication-service/flow-service/graph} and forwards
 * requests to the downstream Flow Service {@code /ubiquia/flow-service/graph}
 * endpoints. The proxying mechanics (method, headers, query params, body) are
 * provided by {@link AbstractUbiquiaDaoControllerProxy}.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Forward {@code /register/post} calls to the Flow Service.</li>
 *   <li>Supply the downstream base URL via {@link #getUrlHelper()} using {@link FlowServiceConfig}.</li>
 * </ul>
 *
 * <p>All I/O is non-blocking and uses the reactive stack; downstream errors propagate from the
 * base proxy implementation as error signals in the returned {@link Mono}.</p>
 */
@RestController
@RequestMapping("/ubiquia/core/communication-service/flow-service/graph")
public class GraphControllerProxy extends AbstractUbiquiaDaoControllerProxy<Graph> {

    /**
     * Host/port configuration for the Flow Service used to build the proxy base URL.
     */
    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Autowired
    private WebClient webClient;

    /**
     * Proxies a graph registration request to the Flow Service.
     *
     * <p>Forwards the incoming POST (including headers and query parameters) to the
     * downstream {@code /register/post} endpoint and returns the downstream response as-is.</p>
     *
     * @param body    the {@link Graph} payload to register
     * @param request the original reactive request whose method/headers/query are mirrored
     * @return a {@link Mono} emitting the downstream {@link ResponseEntity} containing an
     * {@link IngressResponse}, or an error if the call fails
     */
    @PostMapping("/register/post")
    public Mono<ResponseEntity<IngressResponse>> proxyGraphPost(
        @RequestBody Graph body,
        ServerHttpRequest request) {

        var proxied = super.proxyToPostEndpoint("/register/post", request, body);
        return proxied;
    }

    @PostMapping("/deploy")
    public Mono<ResponseEntity<GraphDeployment>> proxyDeploy(
        @RequestBody GraphDeployment body) {

        var url = this.getUrlHelper();
        var uri = UriComponentsBuilder.fromHttpUrl(url + "/deploy")
            .build(true)
            .toUri();

        var response = this
            .webClient
            .method(HttpMethod.POST)
            .uri(uri)
            .bodyValue(body)
            .retrieve()
            .toEntity(GraphDeployment.class);

        return response;
    }

    @PostMapping("/teardown")
    public Mono<ResponseEntity<GraphDeployment>> proxyTeardown(
        @RequestBody GraphDeployment body) {

        var url = this.getUrlHelper();
        var uri = UriComponentsBuilder.fromHttpUrl(url + "/teardown")
            .build(true)
            .toUri();

        var response = this
            .webClient
            .method(HttpMethod.POST)
            .uri(uri)
            .bodyValue(body)
            .retrieve()
            .toEntity(GraphDeployment.class);

        return response;
    }

    /**
     * Builds the base URL for the downstream Flow Service graph endpoints.
     *
     * <p>Example format: {@code http://<host>:<port>/ubiquia/flow-service/graph}</p>
     *
     * @return the fully qualified base URL used by proxy methods
     */
    @Override
    public String getUrlHelper() {
        var url = this.flowServiceConfig.getUrl()
            + ":"
            + this.flowServiceConfig.getPort().toString()
            + "/ubiquia/core/flow-service/graph";
        return url;
    }
}
