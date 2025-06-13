package org.ubiquia.core.belief.state.generator.service.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguageDto;
import org.ubiquia.core.belief.state.generator.service.generator.openapi.OpenApiDtoGenerator;
import org.ubiquia.core.belief.state.generator.service.generator.openapi.OpenApiEntityGenerator;
import org.ubiquia.core.belief.state.generator.service.mapper.JsonSchemaToOpenApiDtoYamlMapper;
import org.ubiquia.core.belief.state.generator.service.mapper.JsonSchemaToOpenApiEntityYamlMapper;

@Service
public class BeliefStateGenerator {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateGenerator.class);

    @Autowired
    private JsonSchemaToOpenApiEntityYamlMapper jsonSchemaToOpenApiEntityYamlMapper;

    @Autowired
    private JsonSchemaToOpenApiDtoYamlMapper jsonSchemaToOpenApiDtoYamlMapper;

    @Autowired
    private OpenApiEntityGenerator openApiEntityGenerator;

    @Autowired
    private OpenApiDtoGenerator openApiDtoGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    public void generateBeliefStateFrom(final AgentCommunicationLanguageDto acl)
        throws JsonProcessingException {

        logger.info("Generating new Belief State from: {}",
            this.objectMapper.writeValueAsString(acl));

        var openApiEntityYaml = this.jsonSchemaToOpenApiEntityYamlMapper.translateJsonSchemaToOpenApiYaml(
            this.objectMapper.writeValueAsString(acl.getJsonSchema())
        );
        this.openApiEntityGenerator.generateOpenApiEntitiesFrom(openApiEntityYaml);

        var openApiDtoYaml = this.jsonSchemaToOpenApiDtoYamlMapper.translateJsonSchemaToOpenApiYaml(
            this.objectMapper.writeValueAsString(acl.getJsonSchema())
        );
        this.openApiDtoGenerator.generateOpenApiDtosFrom(openApiDtoYaml);
    }
}
