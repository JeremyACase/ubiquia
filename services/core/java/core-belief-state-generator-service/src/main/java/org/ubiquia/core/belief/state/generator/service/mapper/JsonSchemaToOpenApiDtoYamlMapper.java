package org.ubiquia.core.belief.state.generator.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.belief.state.generator.controller.BeliefStateGeneratorController;
import org.ubiquia.core.belief.state.generator.model.json.schema.ModelType;
import org.ubiquia.core.belief.state.generator.service.visitor.entity.JsonSchemaModelTypeClassifier;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

@Service
public class JsonSchemaToOpenApiDtoYamlMapper {

    private static final Logger logger =
        LoggerFactory.getLogger(BeliefStateGeneratorController.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JsonSchemaModelTypeClassifier classifier;

    public String translateJsonSchemaToOpenApiYaml(final String jsonSchema)
        throws JsonProcessingException {

        logger.debug("Translating schema to OpenAPI yaml: {}...", jsonSchema);

        // Normalize legacy refs to OpenAPI-style before analyze/transform
        var rewrittenSchema = jsonSchema.replaceAll("#/definitions/", "#/components/schemas/");

        // Classify kinds (ENUM / EMBEDDABLE / ENTITY / ISOLATED) by definition name
        var kindByName = classifier.classify(rewrittenSchema);

        var rootNode = objectMapper.readTree(rewrittenSchema);

        // Prepare OpenAPI structure
        var openApiNode = objectMapper.createObjectNode();
        openApiNode.put("openapi", "3.0.0");

        var infoNode = objectMapper.createObjectNode();
        infoNode.put("title", "Generated OpenAPI");
        infoNode.put("version", "1.0.0");
        openApiNode.set("info", infoNode);

        openApiNode.set("paths", objectMapper.createObjectNode()); // Empty paths

        var componentsNode = objectMapper.createObjectNode();
        var schemasNode = objectMapper.createObjectNode();

        var mainSchema = (ObjectNode) rootNode;

        if (mainSchema.has("definitions")) {
            var definitionsNode = (ObjectNode) mainSchema.get("definitions");

            definitionsNode.fieldNames().forEachRemaining(defName -> {
                var defNode = definitionsNode.get(defName);

                // Deep clone so we don’t mutate the original
                var outNode = deepClone(defNode);

                // If EMBEDDABLE, add the same vendor extension: x-embeddable: true
                var kind = kindByName.getOrDefault(defName, ModelType.ISOLATED);
                if (kind == ModelType.EMBEDDABLE) {
                    ensureObjectNode(outNode).put("x-embeddable", true);
                }

                // Keep DTO names as-is (no +Entity renaming in DTO mapper)
                schemasNode.set(defName, outNode);
            });
        }

        componentsNode.set("schemas", schemasNode);
        openApiNode.set("components", componentsNode);

        var options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        var yaml = new Yaml(options);

        @SuppressWarnings("unchecked")
        var map = objectMapper.convertValue(openApiNode, Map.class);
        var yamlString = yaml.dump(map);

        logger.debug("...generated OpenAPI YAML: \n{}", yamlString);
        return yamlString;
    }

    /* ----------------------- helpers ----------------------- */

    private ObjectNode ensureObjectNode(JsonNode node) {
        if (node instanceof ObjectNode on) return on;
        // Coerce: wrap non-object into an object under "allOf" so we can attach vendor extensions
        var wrapper = objectMapper.createObjectNode();
        var arr = objectMapper.createArrayNode();
        arr.add(node);
        wrapper.set("allOf", arr);
        return wrapper;
    }

    private JsonNode deepClone(JsonNode node) {
        try {
            return objectMapper.readTree(Objects.toString(node));
        } catch (Exception e) {
            return node; // fallback (shouldn’t occur for in-memory nodes)
        }
    }
}
