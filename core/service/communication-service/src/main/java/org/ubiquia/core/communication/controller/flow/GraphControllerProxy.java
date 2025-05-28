package org.ubiquia.core.communication.controller.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.common.models.IngressResponse;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ubiquia/flow")
public class GraphControllerProxy {

    @Value("${ubiquia.flow.service.port}")
    private Integer flowServicePort;

    @Autowired
    private WebClient webClient;

    @PostMapping("/register/post")
    public Mono<ResponseEntity<IngressResponse>> proxyGraphTrigger(@RequestBody Mono<String> body, ServerHttpRequest request) {
        return proxyToFlowService("/register/post", request, body);
    }

    private Mono<ResponseEntity<IngressResponse>> proxyToFlowService(String path, ServerHttpRequest originalRequest, Mono<String> body) {
        var uri = UriComponentsBuilder.fromHttpUrl("http://flow-service/graph" + path)
            .queryParams(originalRequest.getQueryParams())
            .build(true)
            .toUri();

        var response = this
            .webClient
            .method(originalRequest.getMethod())
            .uri(uri)
            .headers(headers -> headers.addAll(originalRequest.getHeaders()))
            .body(body, String.class)
            .retrieve()
            .toEntity(IngressResponse.class);

        return response;
    }
}
