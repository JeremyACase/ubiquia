package org.ubiquia.core.communication.controller.belief;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.common.model.ubiquia.embeddable.BeliefStateGeneration;
import org.ubiquia.core.communication.config.BeliefStateGeneratorServiceConfig;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ubiquia/communication-service/belief-state-generator-service")
public class BeliefStateGeneratorControllerProxy {

    @Autowired
    private BeliefStateGeneratorServiceConfig beliefStateGeneratorServiceConfig;

    @Autowired
    private WebClient webClient;

    @PostMapping("/generate/domain")
    public Mono<ResponseEntity<BeliefStateGeneration>> proxyGenerateDomain(
        @RequestBody Mono<BeliefStateGeneration> body) {

        var url = this.getUrlHelper();
        var uri = UriComponentsBuilder.fromHttpUrl(url + "/generate/domain")
            .build(true)
            .toUri();

        var response = this
            .webClient
            .method(HttpMethod.POST)
            .uri(uri)
            .body(body, BeliefStateGeneration.class)
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
