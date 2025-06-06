package org.ubiquia.core.communication.controller.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.dto.UbiquiaAgentDto;
import org.ubiquia.core.communication.config.FlowServiceConfig;
import org.ubiquia.core.communication.interfaces.InterfaceUbiquiaDaoControllerProxy;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ubiquia/communication-service/flow/ubiquia-agent")
public class UbiquiaAgentControllerProxy implements InterfaceUbiquiaDaoControllerProxy {

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Autowired
    private WebClient webClient;

    @GetMapping("/instance/get")
    public Mono<ResponseEntity<UbiquiaAgentDto>> proxyGetInstance() {

        var targetUri = UriComponentsBuilder
            .fromHttpUrl(this.getUrlHelper() + "/instance/get")
            .build()
            .toUriString();

        return this.webClient
            .get()
            .uri(targetUri)
            .retrieve()
            .toEntity(UbiquiaAgentDto.class)
            .onErrorResume(e -> {
                // Handle downstream service error by returning 502
                return Mono.just(ResponseEntity.status(502).body(null));
            });
    }


    @GetMapping("/{id}/get-deployed-graph-ids")
    public Mono<ResponseEntity<GenericPageImplementation<String>>> proxyGetDeployedGraphIds(
        @PathVariable("id") String agentId,
        @RequestParam(value = "page", defaultValue = "0") Integer page,
        @RequestParam(value = "size", defaultValue = "10") Integer size) {

        var targetUri = UriComponentsBuilder
            .fromHttpUrl(this.getUrlHelper() + "/" + agentId + "/get-deployed-graph-ids")
            .queryParam("page", page)
            .queryParam("size", size)
            .build()
            .toUriString();

        return this.webClient
            .get()
            .uri(targetUri)
            .retrieve()
            .toEntity(new ParameterizedTypeReference<GenericPageImplementation<String>>() {
            })
            .onErrorResume(e -> Mono.just(ResponseEntity.status(502).body(
                new GenericPageImplementation())));
    }

    public String getUrlHelper() {
        var url = this.flowServiceConfig.getUrl()
            + ":"
            + this.flowServiceConfig.getPort().toString()
            + "/ubiquia/ubiquia-agent";
        return url;
    }
}
