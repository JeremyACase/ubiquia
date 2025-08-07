package org.ubiquia.core.communication.controller.belief;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.common.model.ubiquia.IngressResponse;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.model.ubiquia.embeddable.BeliefStateGeneration;
import org.ubiquia.common.library.api.config.BeliefStateGeneratorServiceConfig;
import org.ubiquia.core.communication.interfaces.InterfaceUbiquiaDaoControllerProxy;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ubiquia/communication-service/belief-state-generator-service")
public class BeliefStateGeneratorControllerProxy implements InterfaceUbiquiaDaoControllerProxy {

    @Autowired
    private BeliefStateGeneratorServiceConfig beliefStateGeneratorServiceConfig;

    @Autowired
    private WebClient webClient;

    @PostMapping("/generate/belief-state")
    public Mono<ResponseEntity<BeliefStateGeneration>> proxyGenerate(
        @RequestBody BeliefStateGeneration body) {

        var url = this.getUrlHelper();
        var uri = UriComponentsBuilder.fromHttpUrl(url + "/generate/belief-state")
            .build(true)
            .toUri();

        var response = this
            .webClient
            .method(HttpMethod.POST)
            .uri(uri)
            .bodyValue(body)
            .retrieve()
            .toEntity(BeliefStateGeneration.class);

        return response;
    }

    @PostMapping("/teardown/belief-state")
    public Mono<ResponseEntity<BeliefStateGeneration>> proxyTeardown(
        @RequestBody BeliefStateGeneration body) {

        var url = this.getUrlHelper();
        var uri = UriComponentsBuilder.fromHttpUrl(url + "/teardown/belief-state")
            .build(true)
            .toUri();

        var response = this
            .webClient
            .method(HttpMethod.POST)
            .uri(uri)
            .bodyValue(body)
            .retrieve()
            .toEntity(BeliefStateGeneration.class);

        return response;
    }

    public String getUrlHelper() {
        var url = this.beliefStateGeneratorServiceConfig.getUrl()
            + ":"
            + this.beliefStateGeneratorServiceConfig.getPort().toString()
            + "/belief-state-generator";
        return url;
    }
}
