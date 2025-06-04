package org.ubiquia.core.communication.controller.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.common.model.ubiquia.dto.UbiquiaAgentDto;
import org.ubiquia.core.communication.config.FlowServiceConfig;
import org.ubiquia.core.communication.interfaces.InterfaceUbiquiaDaoControllerProxy;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ubiquia/flow-service/ubiquia-agent")
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

    public String getUrlHelper() {
        var url = this.flowServiceConfig.getUrl()
            + ":"
            + this.flowServiceConfig.getPort().toString()
            + "/ubiquia-agent";
        return url;
    }
}
