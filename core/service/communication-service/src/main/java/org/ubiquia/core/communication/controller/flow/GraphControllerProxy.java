package org.ubiquia.core.communication.controller.flow;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
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

    @Operation(summary = "Upload a graph file")
    @PostMapping(value = "/register/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<IngressResponse>> proxyGraphUpload(
        @Parameter(
            description = "Graph file to upload",
            content = @Content(
                mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                schema = @Schema(type = "string", format = "binary")
            )
        )
        @RequestPart("file") Mono<Part> file,
        ServerHttpRequest request) {

        return proxyToFlowServiceMultipart("/register/upload", request, file);
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

    private Mono<ResponseEntity<IngressResponse>> proxyToFlowServiceMultipart(
        String path,
        ServerHttpRequest originalRequest,
        Mono<Part> fileMono) {

        var url = this.getUrlHelper();

        var uri = UriComponentsBuilder.fromHttpUrl(url + path)
            .queryParams(originalRequest.getQueryParams())
            .build(true)
            .toUri();

        return fileMono.flatMap(file -> {
            MultiValueMap<String, Part> multipartBody = new LinkedMultiValueMap<>();
            multipartBody.add("file", file);

            return this.webClient
                .method(originalRequest.getMethod()) // Should be POST
                .uri(uri)
                .headers(headers -> {
                    headers.addAll(originalRequest.getHeaders());
                    headers.remove("Content-Length");
                    headers.remove("Transfer-Encoding");
                    // Do not set Content-Type here – WebClient will handle it
                })
                .bodyValue(multipartBody)
                .retrieve()
                .toEntity(IngressResponse.class);
        });
    }

    private String getUrlHelper() {
        var url = this.flowServiceUrl
            + this.flowServicePort.toString()
            + "/ubiquia/flow-service/graph";
        return url;
    }
}
