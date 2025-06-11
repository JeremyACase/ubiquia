package org.ubiquia.core.belief.state.generator.service.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguageDto;
import org.ubiquia.core.belief.state.generator.controller.BeliefStateGeneratorController;
import org.ubiquia.core.belief.state.generator.service.mapper.JsonSchemaToOpenApiYamlMapper;

@Service
public class BeliefStateGenerator {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateGenerator.class);

    @Autowired
    private JsonSchemaToOpenApiYamlMapper jsonSchemaToOpenApiYamlMapper;

    @Autowired
    private OpenApiGenerator openApiGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    public void generateBeliefStateFrom(final AgentCommunicationLanguageDto acl)
        throws JsonProcessingException {

        logger.info("Generating new Belief State from: {}",
            this.objectMapper.writeValueAsString(acl));

        var openApiYaml = this.jsonSchemaToOpenApiYamlMapper.translateJsonSchemaToOpenApiYaml(
            this.objectMapper.writeValueAsString(acl.getJsonSchema()));

        this.openApiGenerator.generateOpenApiModelsFrom(openApiYaml);
    }
}
