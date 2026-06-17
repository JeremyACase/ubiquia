package org.ubiquia.core.communication.controller.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.ubiquia.IngressResponse;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import reactor.core.publisher.Mono;

/**
 * Proxy controller for Flow Service graph endpoints.
 *
 * <p>Forwards requests to the downstream Flow Service graph resource. Proxy mechanics
 * are inherited from {@link AbstractUbiquiaDaoControllerProxy}.</p>
 */
@RestController
@RequestMapping("/ubiquia/core/communication-service/flow-service/graph")
public class GraphControllerProxy extends AbstractUbiquiaDaoControllerProxy<Graph> {

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    /** Proxies a graph register-POST to the downstream Flow Service. */
    @PostMapping("/register/post")
    public Mono<ResponseEntity<IngressResponse>> proxyGraphPost(
        @RequestBody final Graph body,
        final ServerHttpRequest request) {
        return super.proxyToPostEndpoint("/register/post", request, body);
    }

    /** Proxies a graph deploy request to the downstream Flow Service. */
    @PostMapping("/deploy")
    public Mono<ResponseEntity<GraphDeployment>> proxyDeploy(
        @RequestBody final GraphDeployment body) {

        var uri = UriComponentsBuilder.fromHttpUrl(this.getUrlHelper() + "/deploy")
            .build(true).toUri();

        return this.webClient
            .method(HttpMethod.POST)
            .uri(uri)
            .bodyValue(body)
            .retrieve()
            .toEntity(GraphDeployment.class);
    }

    /** Proxies a graph teardown request to the downstream Flow Service. */
    @PostMapping("/teardown")
    public Mono<ResponseEntity<GraphDeployment>> proxyTeardown(
        @RequestBody final GraphDeployment body) {

        var uri = UriComponentsBuilder.fromHttpUrl(this.getUrlHelper() + "/teardown")
            .build(true).toUri();

        return this.webClient
            .method(HttpMethod.POST)
            .uri(uri)
            .bodyValue(body)
            .retrieve()
            .toEntity(GraphDeployment.class);
    }

    @Override
    public String getUrlHelper() {
        return this.flowServiceConfig.getBaseUrl() + "/ubiquia/core/flow-service/graph";
    }
}
