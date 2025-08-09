package org.ubiquia.core.belief.state.generator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.library.implementation.service.builder.BeliefStateNameBuilder;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguage;
import org.ubiquia.common.model.ubiquia.embeddable.BeliefStateGeneration;
import org.ubiquia.core.belief.state.generator.service.generator.BeliefStateGenerator;
import org.ubiquia.core.belief.state.generator.service.k8s.BeliefStateOperator;

@RestController
@RequestMapping("belief-state-generator")
public class BeliefStateGeneratorController {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateGeneratorController.class);

    @Autowired
    private BeliefStateGenerator beliefStateGenerator;

    @Autowired
    private BeliefStateNameBuilder beliefStateNameBuilder;

    @Autowired(required = false)
    private BeliefStateOperator beliefStateOperator;

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/belief-state/generate")
    public BeliefStateGeneration generateBeliefState(
        @RequestBody @Valid final BeliefStateGeneration beliefStateGeneration)
        throws Exception {

        logger.info("Received a belief state generation request: {}...",
            this.objectMapper.writeValueAsString(beliefStateGeneration));

        var url = this.flowServiceConfig.getUrl()
            + ":"
            + this.flowServiceConfig.getPort()
            + "/agent-communication-language/query/params";

        var uri = UriComponentsBuilder
            .fromHttpUrl(url)
            .queryParam("page", 0)
            .queryParam("size", 1)
            .queryParam("domain", beliefStateGeneration.getDomainName())
            .queryParam("version.major", beliefStateGeneration.getVersion().getMajor())
            .queryParam("version.minor", beliefStateGeneration.getVersion().getMinor())
            .queryParam("version.patch", beliefStateGeneration.getVersion().getPatch())
            .toUriString();
        logger.debug("...querying URL: {}...", uri);

        var responseType = new ParameterizedTypeReference
            <GenericPageImplementation<AgentCommunicationLanguage>>() {
        };

        var response = this.restTemplate.exchange(
            uri,
            HttpMethod.GET,
            null,
            responseType);

        if (!response.getStatusCode().is2xxSuccessful() ||
            Objects.requireNonNull(response.getBody()).getNumberOfElements() <= 0) {

            throw new IllegalArgumentException("Could not find a registered Agent Communication "
                + "Language for: "
                + this.objectMapper.writeValueAsString(beliefStateGeneration));
        }

        var acl = response.getBody().getContent().get(0);
        this.beliefStateGenerator.generateBeliefStateFrom(acl);

        logger.info("..generated belief state.");

        return beliefStateGeneration;
    }

    @PostMapping("/belief-state/teardown")
    public BeliefStateGeneration teardownBeliefState(
        @RequestBody @Valid final BeliefStateGeneration beliefStateGeneration)
        throws Exception {

        logger.info("Received a belief state teardown request: {}...",
            this.objectMapper.writeValueAsString(beliefStateGeneration));

        if (Objects.nonNull(this.beliefStateOperator)) {
            var beliefStateName = this
                .beliefStateNameBuilder
                .getKubernetesBeliefStateNameFrom(beliefStateGeneration);

            this.beliefStateOperator.deleteBeliefStateResources(beliefStateName);
        }

        return beliefStateGeneration;
    }
}



