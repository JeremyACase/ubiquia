package org.ubiquia.core.communication.controller.flow;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.IngressResponse;
import org.ubiquia.common.model.ubiquia.dto.AbstractModel;
import org.ubiquia.core.communication.interfaces.InterfaceUbiquiaDaoControllerProxy;
import reactor.core.publisher.Mono;

@RestController
public abstract class AbstractUbiquiaDaoControllerProxy<T extends AbstractModel>
    implements InterfaceUbiquiaDaoControllerProxy {

    @Autowired
    private WebClient webClient;

    @GetMapping("/query/params")
    public Mono<ResponseEntity<GenericPageImplementation<T>>> proxyToQueryParams(
        @RequestParam("page") final Integer page,
        @RequestParam("size") final Integer size,
        @RequestParam(value = "sort-descending", required = false, defaultValue = "true") final Boolean sortDescending,
        @RequestParam(value = "sort-by-fields", required = false, defaultValue = "") final List<String> sortByFields,
        HttpServletRequest httpServletRequest) {

        var url = this.getUrlHelper();

        // Create UriComponentsBuilder for the target endpoint
        var targetUriBuilder = UriComponentsBuilder.fromHttpUrl(url + "/query/params");

        // Copy all query parameters from the original request
        httpServletRequest.getParameterMap().forEach((key, values) -> {
            for (var value : values) {
                targetUriBuilder.queryParam(key, value);
            }
        });

        var targetUri = targetUriBuilder.build().toUriString();

        return this.webClient
            .get()
            .uri(targetUri)
            .retrieve()
            .toEntity(new ParameterizedTypeReference<GenericPageImplementation<T>>() {})
            .onErrorResume(e -> Mono.just(ResponseEntity.status(502).body(null)));
    }

    public Mono<ResponseEntity<IngressResponse>> proxyToPostEndpoint(
        String path,
        ServerHttpRequest originalRequest,
        Mono<T> body) {

        var url = this.getUrlHelper();

        var uri = UriComponentsBuilder.fromHttpUrl(url + path)
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
