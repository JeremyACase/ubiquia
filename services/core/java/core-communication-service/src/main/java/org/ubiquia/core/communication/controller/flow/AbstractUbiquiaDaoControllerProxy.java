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
import org.ubiquia.common.model.ubiquia.dto.AbstractModel;
import org.ubiquia.core.communication.interfaces.InterfaceUbiquiaDaoControllerProxy;
import reactor.core.publisher.Mono;

/**
 * Base proxy controller for DAO-style endpoints.
 *
 * <p>This abstract controller provides common proxying behavior for Flow/DAO controllers:
 * it forwards incoming HTTP requests to a downstream service, using a base URL supplied
 * by subclasses via {@code getUrlHelper()} (declared in {@link InterfaceUbiquiaDaoControllerProxy}).</p>
 *
 * <p>All I/O is performed with {@link WebClient} and returns reactive {@link Mono} wrappers.
 * Downstream errors will propagate unless explicitly handled.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Copying/forwarding query parameters to the downstream {@code /query/params} endpoint.</li>
 *   <li>Forwarding POST-like requests with original headers/method/query to arbitrary paths.</li>
 * </ul>
 *
 * <p><strong>Subclass contract:</strong> Implement {@code getUrlHelper()} to return the fully-qualified
 * base URL of the downstream service (e.g., {@code http://host:port/my-service}).</p>
 *
 * @param <T> the concrete {@link AbstractModel} this controller handles
 */
@RestController
public abstract class AbstractUbiquiaDaoControllerProxy<T extends AbstractModel>
    implements InterfaceUbiquiaDaoControllerProxy {

    /**
     * Reactive HTTP client used to forward requests to the downstream service.
     */
    @Autowired
    private WebClient webClient;

    /**
     * Proxies a paginated query to a downstream service by forwarding all query parameters from the
     * incoming request and returning the downstream response as-is.
     *
     * <p>The method:
     * <ol>
     *   <li>Builds a target URL based on {@link #getUrlHelper()} and the downstream path {@code /query/params}.</li>
     *   <li>Copies <em>all</em> query parameters from the incoming {@link ServerHttpRequest} (including
     *       repeated keys / multi-valued params) onto the target URL, preserving existing request
     *       parameters such as {@code page}, {@code size}, {@code sort-descending}, and {@code sort-by-fields}.</li>
     *   <li>Issues an HTTP GET via {@link org.springframework.web.reactive.function.client.WebClient} and
     *       maps the body to {@code GenericPageImplementation<T>}.</li>
     *   <li>On any client error (network/codec/downstream error), returns {@code 502 Bad Gateway} with a null body.</li>
     * </ol>
     *
     * <p><strong>Notes:</strong>
     * <ul>
     *   <li>{@link org.springframework.web.util.UriComponentsBuilder#build(boolean) build(true)} is used to
     *       preserve any percent-encoding already present in parameters.</li>
     *   <li>The explicit {@code @RequestParam} arguments are validated/bound by Spring for local defaults,
     *       but the forwarded call relies solely on the parameters present in the incoming request.</li>
     * </ul>
     *
     * @param page           The zero- or one-based page index requested by the client (forwarded downstream).
     * @param size           The page size requested by the client (forwarded downstream).
     * @param sortDescending Whether sorting should be descending; defaults to {@code true} if omitted (forwarded downstream).
     * @param sortByFields   Optional list of field names to sort by; defaults to an empty list if omitted (forwarded downstream).
     * @param request        The reactive HTTP request whose query parameters are forwarded verbatim.
     * @return a {@link reactor.core.publisher.Mono} emitting a {@link org.springframework.http.ResponseEntity}
     * containing the downstream page {@code GenericPageImplementation<T>}; emits {@code 502 Bad Gateway}
     * with {@code null} body if the downstream call fails.
     * @implNote All query parameters from the inbound request are appended to the target URI. If there is any
     * overlap between explicitly declared method parameters and raw query parameters, the values in
     * the incoming request are forwarded; this method does not reconcile or rewrite keys/values.
     * @see org.springframework.web.util.UriComponentsBuilder
     * @see org.springframework.web.reactive.function.client.WebClient
     */
    @GetMapping("/query/params")
    public Mono<ResponseEntity<GenericPageImplementation<T>>> proxyToQueryParams(
        @RequestParam("page") final Integer page,
        @RequestParam("size") final Integer size,
        @RequestParam(value = "sort-descending", required = false, defaultValue = "true") final Boolean sortDescending,
        @RequestParam(value = "sort-by-fields", required = false, defaultValue = "") final List<String> sortByFields,
        @Parameter(hidden = true) final HttpServletRequest request
    ) {
        var url = this.getUrlHelper();

        var targetUriBuilder = UriComponentsBuilder.fromHttpUrl(url + "/query/params");
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
            .toEntity(new ParameterizedTypeReference<GenericPageImplementation<T>>() {
            })
            .onErrorResume(e -> Mono.just(ResponseEntity.status(502).body(null)));
    }

    /**
     * Generic proxy for POST-like endpoints (also supports PUT/DELETE if present on the original request).
     *
     * <p>Constructs the target URI as {@code getUrlHelper() + path} and copies:
     * <ul>
     *   <li>HTTP method from {@code originalRequest}</li>
     *   <li>All query parameters from {@code originalRequest}</li>
     *   <li>All headers from {@code originalRequest}</li>
     *   <li>Request body as {@code T}</li>
     * </ul>
     * The downstream response is deserialized into an {@link IngressResponse}.</p>
     *
     * @param path            the downstream path (e.g., {@code /entity/create})
     * @param originalRequest the original reactive request whose method/headers/query are mirrored
     * @param body            the request payload to forward
     * @return a {@link Mono} that emits the downstream {@link ResponseEntity} containing an
     * {@link IngressResponse}
     */
    public Mono<ResponseEntity<IngressResponse>> proxyToPostEndpoint(
        String path,
        ServerHttpRequest originalRequest,
        T body) {

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
            .bodyValue(body)
            .retrieve()
            .toEntity(IngressResponse.class);

        return response;
    }
}
