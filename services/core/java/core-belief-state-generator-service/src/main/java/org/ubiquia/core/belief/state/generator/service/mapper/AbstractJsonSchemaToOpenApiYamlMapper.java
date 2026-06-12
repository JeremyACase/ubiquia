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
import org.ubiquia.core.belief.state.generator.model.json.schema.ModelType;
import org.ubiquia.core.belief.state.generator.service.visitor.entity.JsonSchemaModelTypeClassifier;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Common scaffolding for translating a JSON Schema string into an OpenAPI 3.0 YAML document.
 *
 * <p>The flow is: normalize {@code $ref} paths → classify definitions → build the OpenAPI
 * skeleton → call {@link #preprocessDefinitions} → visit each definition via
 * {@link #visitDefinition} → serialize to YAML.
 *
 * <p>Subclasses implement {@link #visitDefinition} to apply type-specific naming and vendor
 * extensions. They may also override {@link #preprocessDefinitions} to rewrite {@code $ref}
 * targets or add cross-definition annotations before iteration starts.
 */
public abstract class AbstractJsonSchemaToOpenApiYamlMapper {

    private static final Logger logger =
        LoggerFactory.getLogger(AbstractJsonSchemaToOpenApiYamlMapper.class);

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JsonSchemaModelTypeClassifier classifier;

    /**
     * Translates {@code jsonSchema} to an OpenAPI 3.0 YAML string.
     *
     * @param jsonSchema the input JSON Schema string
     * @return the OpenAPI YAML string
     * @throws JsonProcessingException if the schema cannot be parsed
     */
    public String translateJsonSchemaToOpenApiYaml(final String jsonSchema)
        throws JsonProcessingException {

        logger.debug("Translating schema to OpenAPI yaml: {}...", jsonSchema);

        var rewrittenSchema =
            jsonSchema.replaceAll("#/definitions/", "#/components/schemas/");
        var kindByName = this.classifier.classify(rewrittenSchema);

        var mainSchema = (ObjectNode) this.objectMapper.readTree(rewrittenSchema);

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

            this.preprocessDefinitions(mainSchema, kindByName);

            definitionsNode.fieldNames().forEachRemaining(defName -> {
                var kind = kindByName.getOrDefault(defName, ModelType.ISOLATED);
                this.visitDefinition(
                    defName, this.deepClone(definitionsNode.get(defName)), kind, schemasNode);
            });
        }

        componentsNode.set("schemas", schemasNode);
        openApiNode.set("components", componentsNode);

        var options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        @SuppressWarnings("unchecked")
        var map = this.objectMapper.convertValue(openApiNode, Map.class);
        var yamlString = new Yaml(options).dump(map);

        logger.debug("...generated OpenAPI YAML: \n{}", yamlString);
        return yamlString;
    }

    /**
     * Called once before definition iteration; subclasses may rewrite {@code $ref} values
     * or add cross-definition annotations here.
     *
     * <p>Default implementation is a no-op.
     *
     * @param mainSchema the root schema node (mutable)
     * @param kindByName definition name to classified {@link ModelType}
     */
    protected void preprocessDefinitions(
        final ObjectNode mainSchema,
        final Map<String, ModelType> kindByName) {
        // default: no preprocessing
    }

    /**
     * Visits a single definition and writes it (possibly renamed or annotated) into
     * {@code schemasNode}.
     *
     * @param defName     the original definition name
     * @param defNode     a deep clone of the definition node, safe to mutate
     * @param kind        the classified {@link ModelType} for this definition
     * @param schemasNode the OpenAPI {@code components/schemas} node to write into
     */
    protected abstract void visitDefinition(
        String defName,
        JsonNode defNode,
        ModelType kind,
        ObjectNode schemasNode);

    /**
     * Returns {@code node} as an {@link ObjectNode}, wrapping it in an {@code allOf} container
     * if necessary so that vendor extensions can be attached.
     *
     * @param node the node to coerce
     * @return the node itself if it is already an ObjectNode, otherwise a wrapping object
     */
    protected ObjectNode ensureObjectNode(final JsonNode node) {
        if (node instanceof ObjectNode on) {
            return on;
        }
        var wrapper = this.objectMapper.createObjectNode();
        var arr = this.objectMapper.createArrayNode();
        arr.add(node);
        wrapper.set("allOf", arr);
        return wrapper;
    }

    /**
     * Returns a deep clone of {@code node} that is safe to mutate without affecting the original.
     *
     * @param node the node to clone
     * @return a deep copy, or the original node if cloning unexpectedly fails
     */
    protected JsonNode deepClone(final JsonNode node) {
        try {
            return this.objectMapper.readTree(Objects.toString(node));
        } catch (Exception e) {
            return node;
        }
    }
}
