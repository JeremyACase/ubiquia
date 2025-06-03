package org.ubiquia.core.communication.controller.flow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.ubiquia.common.model.ubiquia.IngressResponse;
import org.ubiquia.common.model.ubiquia.dto.GraphDto;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ubiquia/flow-service/graph")
public class GraphControllerProxy
    extends AbstractUbiquiaDaoControllerProxy<GraphDto> {

    @Value("${ubiquia.flow.service.url:http://ubiquia-core-flow-service:}")
    private String serviceUrl;

    @Value("${ubiquia.flow.service.port:8080}")
    private Integer servicePort;

    @PostMapping("/register/post")
    public Mono<ResponseEntity<IngressResponse>> proxyGraphPost(
        @RequestBody Mono<GraphDto> body,
        ServerHttpRequest request) {

        var proxied = super.proxyToPostEndpoint("/register/post", request, body);
        return proxied;
    }

    public String getUrlHelper() {
        var url = this.serviceUrl
            + this.servicePort.toString()
            + "/ubiquia/flow-service/graph";
        return url;
    }
}
