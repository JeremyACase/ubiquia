package org.ubiquia.core.communication.controller.flow;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.ubiquia.common.library.dao.model.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.IngressResponse;
import org.ubiquia.common.model.ubiquia.dto.AbstractEntityDto;
import org.ubiquia.core.communication.interfaces.InterfaceUbiquiaDaoControllerProxy;
import reactor.core.publisher.Mono;

@RestController
public abstract class AbstractUbiquiaDaoControllerProxy<T extends AbstractEntityDto>
    implements InterfaceUbiquiaDaoControllerProxy {

    @Autowired
    private WebClient webClient;

    @GetMapping("/query/params")
    public Mono<ResponseEntity<GenericPageImplementation<T>>> proxyToQueryParams(
        final Integer page,
        final Integer size,
        final Boolean sortDescending,
        final List<String> sortByFields,
        HttpServletRequest httpServletRequest) {

        var url = this.getUrlHelper();

        // Build the target URI with query params
        var targetUri = UriComponentsBuilder
            .fromHttpUrl(url + "/query/params")
            .queryParam("page", page)
            .queryParam("size", size)
            .queryParam("sort-descending", sortDescending)
            .queryParam("sort-by-fields", String.join(",", sortByFields))
            .build()
            .toUriString();

        return this.webClient
            .get()
            .uri(targetUri)
            .retrieve()
            .toEntity(new ParameterizedTypeReference<GenericPageImplementation<T>>() {})
            .onErrorResume(e -> {
                // Map error to a 502 Bad Gateway if the downstream service fails
                return Mono.just(ResponseEntity.status(502).body(null));
            });
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
