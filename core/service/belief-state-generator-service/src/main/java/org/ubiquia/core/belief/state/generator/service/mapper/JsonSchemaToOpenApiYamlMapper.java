package org.ubiquia.core.belief.state.generator.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.belief.state.generator.controller.BeliefStateGeneratorController;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

@Service
public class JsonSchemaToOpenApiYamlMapper {


    private static final Logger logger = LoggerFactory.getLogger(BeliefStateGeneratorController.class);

    @Autowired
    private ObjectMapper objectMapper;

    public String translateJsonSchemaToOpenApiYaml(final String jsonSchema)
        throws JsonProcessingException {
        logger.debug("Translating schema to OpenAPI yaml: {}...", jsonSchema);

        var jsonNode = this.objectMapper.readTree(jsonSchema);

        var openApiNode = this.objectMapper.createObjectNode();
        openApiNode.put("openapi", "3.0.0");

        var infoNode = this.objectMapper.createObjectNode();
        infoNode.put("title", "Generated OpenAPI");
        infoNode.put("version", "1.0.0");

        openApiNode.set("info", infoNode);

        var pathsNode = this.objectMapper.createObjectNode();
        openApiNode.set("paths", pathsNode);

        var componentsNode = this.objectMapper.createObjectNode();
        var schemasNode = this.objectMapper.createObjectNode();
        schemasNode.set("GeneratedSchema", jsonNode);
        componentsNode.set("schemas", schemasNode);

        openApiNode.set("components", componentsNode);

        // Convert the final OpenAPI JSON object to YAML
        var options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        var yaml = new Yaml(options);

        // Convert ObjectNode to a Map for SnakeYAML
        var map = this.objectMapper.convertValue(openApiNode, Map.class);
        var yamlString = yaml.dump(map);

        logger.debug("...generated OpenAPI YAML: \n{}", yamlString);

        return yamlString;
    }
}
