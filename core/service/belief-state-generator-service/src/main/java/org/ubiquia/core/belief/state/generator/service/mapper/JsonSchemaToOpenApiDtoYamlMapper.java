package org.ubiquia.core.belief.state.generator.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.belief.state.generator.controller.BeliefStateGeneratorController;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

@Service
public class JsonSchemaToOpenApiDtoYamlMapper {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateGeneratorController.class);

    @Autowired
    private ObjectMapper objectMapper;

    public String translateJsonSchemaToOpenApiYaml(final String jsonSchema) throws JsonProcessingException {
        logger.debug("Translating schema to OpenAPI yaml: {}...", jsonSchema);

        // Rewrite legacy $ref paths to OpenAPI-style
        var rewrittenSchema = jsonSchema.replaceAll("#/definitions/", "#/components/schemas/");

        var rootNode = this.objectMapper.readTree(rewrittenSchema);
        ObjectNode mainSchema = (ObjectNode) rootNode;

        // Prepare OpenAPI skeleton
        var openApiNode = this.objectMapper.createObjectNode();
        openApiNode.put("openapi", "3.0.0");

        var infoNode = this.objectMapper.createObjectNode();
        infoNode.put("title", "Generated OpenAPI");
        infoNode.put("version", "1.0.0");
        openApiNode.set("info", infoNode);

        openApiNode.set("paths", this.objectMapper.createObjectNode());

        var componentsNode = this.objectMapper.createObjectNode();
        var schemasNode = this.objectMapper.createObjectNode();

        if (mainSchema.has("definitions")) {
            var definitionsNode = (ObjectNode) mainSchema.get("definitions");

            // First update all $ref to use Dto suffix
            replaceRefsWithDto(mainSchema, definitionsNode);

            // Then rename all schema definitions to Dto
            definitionsNode.fieldNames().forEachRemaining(defName -> {
                var defNode = definitionsNode.get(defName);
                if (this.isEnumDefinition(defNode)) {
                    schemasNode.set(defName, defNode); // enums: keep original name
                } else {
                    schemasNode.set(defName + "Dto", defNode); // others: add Dto
                }
            });
        }

        componentsNode.set("schemas", schemasNode);
        openApiNode.set("components", componentsNode);

        // Convert to YAML
        var options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        var yaml = new Yaml(options);

        var map = this.objectMapper.convertValue(openApiNode, Map.class);
        var yamlString = yaml.dump(map);

        logger.debug("...generated OpenAPI YAML: \n{}", yamlString);

        return yamlString;
    }

    private void replaceRefsWithDto(JsonNode node, ObjectNode definitionsNode) {
        if (node.isObject()) {
            var objNode = (ObjectNode) node;
            objNode.fields().forEachRemaining(entry -> {
                var key = entry.getKey();
                var value = entry.getValue();

                if ("$ref".equals(key) && value.isTextual()) {
                    var ref = value.asText();
                    if (ref.startsWith("#/components/schemas/")) {
                        var schemaName = ref.substring("#/components/schemas/".length());

                        JsonNode targetDef = definitionsNode.get(schemaName);
                        if (targetDef != null && this.isEnumDefinition(targetDef)) {
                            objNode.put("$ref", "#/components/schemas/" + schemaName); // no Dto for enums
                        } else {
                            objNode.put("$ref", "#/components/schemas/" + schemaName + "Dto");
                        }
                    }
                } else {
                    this.replaceRefsWithDto(value, definitionsNode);
                }
            });
        } else if (node.isArray()) {
            for (var item : node) {
                this.replaceRefsWithDto(item, definitionsNode);
            }
        }
    }

    private boolean isEnumDefinition(JsonNode defNode) {
        return defNode.has("enum") && defNode.get("enum").isArray()
            && defNode.has("type") && "string".equals(defNode.get("type").asText());
    }
}
