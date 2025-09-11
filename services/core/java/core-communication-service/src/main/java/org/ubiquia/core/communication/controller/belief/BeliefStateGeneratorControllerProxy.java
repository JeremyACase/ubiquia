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
import org.ubiquia.common.library.api.config.BeliefStateGeneratorServiceConfig;
import org.ubiquia.common.model.ubiquia.embeddable.BeliefStateGeneration;
import org.ubiquia.core.communication.interfaces.InterfaceUbiquiaDaoControllerProxy;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ubiquia/communication-service/belief-state-generator-service")
public class BeliefStateGeneratorControllerProxy implements InterfaceUbiquiaDaoControllerProxy {

    @Autowired
    private BeliefStateGeneratorServiceConfig beliefStateGeneratorServiceConfig;

    @Autowired
    private WebClient webClient;

    /**
     * Proxies a request to create (build/package/deploy) a Belief State to the
     * Belief State Generator service.
     *
     * <p>Builds the downstream URI from {@link BeliefStateGeneratorServiceConfig}
     * and forwards a POST to {@code /belief-state/generate}, returning the
     * downstream response as-is.</p>
     *
     * <p>This method is non-blocking and relies on the configured
     * {@link WebClient} for I/O, timeouts, and error handling. Any 4xx/5xx from
     * the downstream service will surface as an error in the returned {@link Mono}.</p>
     *
     * @param body the {@link BeliefStateGeneration} request payload describing what to generate
     * @return a {@link Mono} emitting the downstream {@link ResponseEntity} with the
     * resulting {@link BeliefStateGeneration}, or an error if the call fails
     */
    @PostMapping("/belief-state/generate")
    public Mono<ResponseEntity<BeliefStateGeneration>> proxyGenerate(
        @RequestBody BeliefStateGeneration body) {

        var url = this.getUrlHelper();
        var uri = UriComponentsBuilder.fromHttpUrl(url + "/belief-state/generate")
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

    /**
     * Proxies a request to tear down (undeploy/cleanup) a previously generated
     * Belief State via the Belief State Generator service.
     *
     * <p>Builds the downstream URI from {@link BeliefStateGeneratorServiceConfig}
     * and forwards a POST to {@code /belief-state/teardown}, returning the
     * downstream response as-is.</p>
     *
     * <p>This method is non-blocking and relies on the configured
     * {@link WebClient}. Downstream errors (4xx/5xx) propagate as an error in
     * the returned {@link Mono}.</p>
     *
     * @param body the {@link BeliefStateGeneration} identifying what to tear down
     * @return a {@link Mono} emitting the downstream {@link ResponseEntity} with the
     * resulting {@link BeliefStateGeneration}, or an error if the call fails
     */
    @PostMapping("/belief-state/teardown")
    public Mono<ResponseEntity<BeliefStateGeneration>> proxyTeardown(
        @RequestBody BeliefStateGeneration body) {

        var url = this.getUrlHelper();
        var uri = UriComponentsBuilder.fromHttpUrl(url + "/belief-state/teardown")
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

    /**
     * Builds the base URL for the Belief State Generator service from the configured
     * host and port, and appends the service context path.
     *
     * <p>Example format: {@code http://host:port/belief-state-generator}</p>
     *
     * @return the fully-qualified base URL used by proxy methods
     */
    public String getUrlHelper() {
        var url = this.beliefStateGeneratorServiceConfig.getUrl()
            + ":"
            + this.beliefStateGeneratorServiceConfig.getPort().toString()
            + "/belief-state-generator";
        return url;
    }
}
