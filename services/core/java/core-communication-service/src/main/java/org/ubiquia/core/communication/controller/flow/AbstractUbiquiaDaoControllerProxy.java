package org.ubiquia.core.communication.controller.flow;

import io.swagger.v3.oas.annotations.Parameter;
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
import org.ubiquia.core.communication.interfaces.InterfaceUbiquiaDaoControllerProxy;
import reactor.core.publisher.Mono;

/**
 * Base proxy controller for DAO-style endpoints.
 *
 * <p>Forwards requests to a downstream Flow Service using the base URL supplied by
 * subclasses via {@link #getUrlHelper()}. All I/O is reactive via {@link WebClient}.</p>
 *
 * @param <T> the model type this controller handles
 */
@RestController
public abstract class AbstractUbiquiaDaoControllerProxy<T>
    implements InterfaceUbiquiaDaoControllerProxy {

    @Autowired
    protected WebClient webClient;

    /**
     * Proxies a paginated query, forwarding all incoming query params to the downstream
     * {@code /query/params} endpoint.
     */
    @GetMapping("/query/params")
    public Mono<ResponseEntity<GenericPageImplementation<T>>> proxyToQueryParams(
        @RequestParam("page") final Integer page,
        @RequestParam("size") final Integer size,
        @RequestParam(value = "sort-descending", required = false, defaultValue = "true")
        final Boolean sortDescending,
        @RequestParam(value = "sort-by-fields", required = false, defaultValue = "")
        final List<String> sortByFields,
        @Parameter(hidden = true) final HttpServletRequest request
    ) {
        var targetUriBuilder = UriComponentsBuilder
            .fromHttpUrl(this.getUrlHelper() + "/query/params");
        request.getParameterMap().forEach((key, values) -> {
            for (var v : values) {
                targetUriBuilder.queryParam(key, v);
            }
        });
        var targetUri = targetUriBuilder.build(true).toUriString();

        return this.webClient
            .get()
            .uri(targetUri)
            .retrieve()
            .toEntity(new ParameterizedTypeReference<GenericPageImplementation<T>>() {})
            .onErrorResume(e -> Mono.just(ResponseEntity.status(502).body(null)));
    }

    /**
     * Proxies a POST/PUT/DELETE to the downstream service, mirroring method, headers,
     * query parameters, and body.
     *
     * @param path            downstream path to append to the base URL
     * @param originalRequest source of method, headers, and query params
     * @param body            request payload to forward
     */
    public Mono<ResponseEntity<IngressResponse>> proxyToPostEndpoint(
        final String path,
        final ServerHttpRequest originalRequest,
        final T body) {

        var uri = UriComponentsBuilder.fromHttpUrl(this.getUrlHelper() + path)
            .queryParams(originalRequest.getQueryParams())
            .build(true)
            .toUri();

        return this.webClient
            .method(originalRequest.getMethod())
            .uri(uri)
            .headers(headers -> headers.addAll(originalRequest.getHeaders()))
            .bodyValue(body)
            .retrieve()
            .toEntity(IngressResponse.class);
    }
}
