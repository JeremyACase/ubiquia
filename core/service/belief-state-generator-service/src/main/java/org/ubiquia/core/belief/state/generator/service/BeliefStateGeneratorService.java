package org.ubiquia.core.belief.state.generator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguageDto;
import org.ubiquia.core.belief.state.generator.controller.BeliefStateGeneratorController;

@Service
public class BeliefStateGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateGeneratorController.class);

    @Autowired
    private JsonSchemaToOpenApiYamlService jsonSchemaToOpenApiYamlService;

    @Autowired
    private ObjectMapper objectMapper;

    public void generateBeliefStateFrom(final AgentCommunicationLanguageDto acl)
        throws JsonProcessingException {

        logger.info("Generating new Belief State from: {}",
            this.objectMapper.writeValueAsString(acl));

        var yaml = this.jsonSchemaToOpenApiYamlService.translateJsonSchemaToOpenApiYaml(
            this.objectMapper.writeValueAsString(acl.getJsonSchema()));

        logger.debug("Generated YAML: {}", yaml);
    }

}
