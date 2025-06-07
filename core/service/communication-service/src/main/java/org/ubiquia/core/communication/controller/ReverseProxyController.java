package org.ubiquia.core.communication.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.ubiquia.core.communication.service.manager.flow.AdapterProxyManager;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/proxy")
public class ReverseProxyController {

    private static final Logger logger = LoggerFactory.getLogger(ReverseProxyController.class);

    @Autowired
    private AdapterProxyManager adapterProxyManager;

    @Autowired
    private WebClient webClient;

    @RequestMapping(value = "/{adapterName}/**",
        method = {
            RequestMethod.GET,
            RequestMethod.POST,
            RequestMethod.PUT}
    )
    public Mono<ResponseEntity<byte[]>> proxyToAdapter(
        @PathVariable String adapterName,
        ServerHttpRequest request,
        ServerHttpResponse response) {

        // Find the target base URL
        var adapterUris = adapterProxyManager.getProxiedAdapterMap().get(adapterName);
        if (adapterUris == null || adapterUris.isEmpty()) {
            return Mono.just(ResponseEntity.notFound().build());
        }

        var targetBaseUrl = adapterUris.get(0).toString();

        // Reconstruct the target URI
        var path = request.getURI().getRawPath().replaceFirst("/proxy/" + adapterName, "");
        var targetUri = targetBaseUrl
            + path
            + (request.getURI().getRawQuery() != null ? "?"
            + request.getURI().getRawQuery() : "");

        logger.info("Proxying to adapter {} -> {}", adapterName, targetUri);

        // Prepare the WebClient request
        var requestSpec = this.webClient.method(request.getMethod())
            .uri(targetUri)
            .headers(httpHeaders -> {
                httpHeaders.addAll(request.getHeaders());
                httpHeaders.remove(HttpHeaders.HOST); // remove host header to avoid confusion
            });

        // Forward the body if present
        var bodyMono = request.getBody()
            .map(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                return bytes;
            })
            .reduce(this::concat);

        return bodyMono
            .flatMap(body -> requestSpec.bodyValue(body).exchangeToMono(resp -> resp.toEntity(byte[].class)))
            .switchIfEmpty(requestSpec.exchangeToMono(resp -> resp.toEntity(byte[].class)));
    }

    private byte[] concat(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}
