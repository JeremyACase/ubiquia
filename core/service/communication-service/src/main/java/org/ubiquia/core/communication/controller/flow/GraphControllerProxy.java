package org.ubiquia.core.communication.controller.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.model.ubiquia.IngressResponse;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.core.communication.config.FlowServiceConfig;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ubiquia/communication-service/flow-service/graph")
public class GraphControllerProxy
    extends AbstractUbiquiaDaoControllerProxy<Graph> {

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @PostMapping("/register/post")
    public Mono<ResponseEntity<IngressResponse>> proxyGraphPost(
        @RequestBody Mono<Graph> body,
        ServerHttpRequest request) {

        var proxied = super.proxyToPostEndpoint("/register/post", request, body);
        return proxied;
    }

    public String getUrlHelper() {
        var url = this.flowServiceConfig.getUrl()
            + ":"
            + this.flowServiceConfig.getPort().toString()
            + "/ubiquia/flow-service/graph";
        return url;
    }
}
