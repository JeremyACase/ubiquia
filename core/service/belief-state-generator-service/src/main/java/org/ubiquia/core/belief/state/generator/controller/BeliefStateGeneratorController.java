package org.ubiquia.core.belief.state.generator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguage;
import org.ubiquia.common.model.ubiquia.embeddable.DomainGeneration;
import org.ubiquia.core.belief.state.generator.service.generator.BeliefStateGenerator;
import org.ubiquia.core.communication.config.FlowServiceConfig;

@RestController
@RequestMapping("belief-state-generator")
public class BeliefStateGeneratorController {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateGeneratorController.class);

    @Autowired
    private BeliefStateGenerator beliefStateGenerator;

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/generate/domain")
    @Transactional
    public DomainGeneration generateDomain(@RequestBody @Valid final DomainGeneration domainGeneration)
        throws Exception {

        logger.info("Received a domain generation request: {}...",
            this.objectMapper.writeValueAsString(domainGeneration));

        var url = this.flowServiceConfig.getUrl()
            + ":"
            + this.flowServiceConfig.getPort()
            + "/agent-communication-language/query/params";

        var uri = UriComponentsBuilder
            .fromHttpUrl(url)
            .queryParam("page", 0)
            .queryParam("size", 1)
            .queryParam("domain", domainGeneration.getName())
            .queryParam("version.major", domainGeneration.getVersion().getMajor())
            .queryParam("version.minor", domainGeneration.getVersion().getMinor())
            .queryParam("version.patch", domainGeneration.getVersion().getPatch())
            .toUriString();
        logger.debug("...querying URL: {}...", uri);

        var responseType = new ParameterizedTypeReference
            <GenericPageImplementation<AgentCommunicationLanguage>>() {
        };

        var response =
            this.restTemplate.exchange(uri, HttpMethod.GET, null, responseType);

        if (!response.getStatusCode().is2xxSuccessful() ||
            Objects.requireNonNull(response.getBody()).getNumberOfElements() <= 0) {

            throw new IllegalArgumentException("Could not find a registered Agent Communication "
                + "Language for: " + this.objectMapper.writeValueAsString(domainGeneration));
        }

        var acl = response.getBody().getContent().get(0);
        this.beliefStateGenerator.generateBeliefStateFrom(acl);

        return domainGeneration;
    }
}



