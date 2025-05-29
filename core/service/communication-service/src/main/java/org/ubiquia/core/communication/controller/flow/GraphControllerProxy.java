package org.ubiquia.core.communication.controller.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
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

    @Value("${ubiquia.flow.service.url:http://ubiquia-core-flow-service:}")
    private String flowServiceUrl;

    @Value("${ubiquia.flow.service.port}")
    private Integer flowServicePort;

    @Autowired
    private WebClient webClient;

    @PostMapping("/register/post")
    public Mono<ResponseEntity<IngressResponse>> proxyGraphPost(
        @RequestBody Mono<String> body,
        ServerHttpRequest request) {
        return proxyToFlowService("/register/post", request, body);
    }

    @PostMapping("/register/upload")
    public Mono<ResponseEntity<IngressResponse>> proxyGraphUpload(
        @RequestBody Mono<MultiValueMap<String, Part>> multipartBody,
        ServerHttpRequest request) {

        return proxyToFlowServiceMultipart("/register/upload", request, multipartBody);
    }

    private Mono<ResponseEntity<IngressResponse>> proxyToFlowService(
        String path,
        ServerHttpRequest originalRequest,
        Mono<String> body) {

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

    public Mono<ResponseEntity<IngressResponse>> proxyToFlowServiceMultipart(
        String path,
        ServerHttpRequest originalRequest,
        Mono<MultiValueMap<String, Part>> multipartBody) {

        var url = this.getUrlHelper();

        var uri = UriComponentsBuilder.fromHttpUrl(url + path)
            .queryParams(originalRequest.getQueryParams())
            .build(true)
            .toUri();

        var response = this.webClient
            .method(originalRequest.getMethod()) // Should be POST
            .uri(uri)
            .headers(headers -> {
                headers.addAll(originalRequest.getHeaders());
                // Remove Transfer-Encoding: chunked, which can confuse some servers
                headers.remove("Transfer-Encoding");
                // Remove Content-Length if present (WebClient calculates it automatically for multipart)
                headers.remove("Content-Length");
                // Set the content type to multipart/form-data
                headers.setContentType(null); // Let WebClient derive it from BodyInserter
            })
            .body(multipartBody, new ParameterizedTypeReference<>() {
            })
            .retrieve()
            .toEntity(IngressResponse.class);

        return response;
    }

    private String getUrlHelper() {
        var url = this.flowServiceUrl
            + this.flowServicePort.toString()
            + "/ubiquia/flow-service/graph";
        return url;
    }
}
