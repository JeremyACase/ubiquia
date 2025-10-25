package org.ubiquia.core.belief.state.generator.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.belief.state.generator.model.json.schema.ModelType;
import org.ubiquia.core.belief.state.generator.service.visitor.entity.JsonSchemaModelTypeClassifier;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

@Service
public class JsonSchemaToOpenApiEntityYamlMapper {

    private static final Logger logger =
        LoggerFactory.getLogger(JsonSchemaToOpenApiEntityYamlMapper.class);

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JsonSchemaModelTypeClassifier classifier;

    public String translateJsonSchemaToOpenApiYaml(final String jsonSchema)
        throws JsonProcessingException {
        logger.debug("Translating schema to OpenAPI yaml: {}...", jsonSchema);

        // Normalize legacy refs to OpenAPI-style before we analyze/transform
        final var rewrittenSchema =
            jsonSchema.replaceAll("#/definitions/", "#/components/schemas/");

        // Classify using the (normalized) schema
        final var kindByName = classifier.classify(rewrittenSchema);

        final var rootNode = objectMapper.readTree(rewrittenSchema);
        final var mainSchema = (ObjectNode) rootNode;

        // ---------- OpenAPI Skeleton ----------
        final var openApiNode = objectMapper.createObjectNode();
        openApiNode.put("openapi", "3.0.0");

        final var infoNode = objectMapper.createObjectNode();
        infoNode.put("title", "Generated OpenAPI");
        infoNode.put("version", "1.0.0");
        openApiNode.set("info", infoNode);
        openApiNode.set("paths", objectMapper.createObjectNode());

        final var componentsNode = objectMapper.createObjectNode();
        final var schemasNode = objectMapper.createObjectNode();

        if (mainSchema.has("definitions")) {
            final var definitionsNode = (ObjectNode) mainSchema.get("definitions");

            // 1) Rewrite $refs with proper *Entity suffixing, etc.
            rewriteRefsByKind(mainSchema, kindByName);

            // 2) Annotate properties that reference embeddables
            annotateElementCollectionsOnFields(definitionsNode, kindByName);

            // 3) Emit schemas
            definitionsNode
                .fieldNames()
                .forEachRemaining(
                    defName -> {
                        final var defNode = definitionsNode.get(defName);
                        final var kind =
                            kindByName.getOrDefault(defName, ModelType.ISOLATED);

                        final var outNode = deepClone(defNode);

                        switch (kind) {
                            case ENUM:
                                schemasNode.set(defName, outNode);
                                break;
                            case EMBEDDABLE:
                                ensureObjectNode(outNode).put("x-embeddable", true);
                                schemasNode.set(defName + "Entity", outNode);
                                break;
                            default:
                                schemasNode.set(defName + "Entity", outNode);
                                break;
                        }
                    });
        }

        componentsNode.set("schemas", schemasNode);
        openApiNode.set("components", componentsNode);

        // ---------- Convert to YAML ----------
        final var options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        final var yaml = new Yaml(options);

        @SuppressWarnings("unchecked") final var map = objectMapper.convertValue(openApiNode, Map.class);
        final var yamlString = yaml.dump(map);

        logger.debug("...generated OpenAPI YAML: \n{}", yamlString);
        return yamlString;
    }

    /**
     * Marks fields that reference EMBEDDABLE types with "x-element-collection": true.
     * Works for:
     * - direct $ref
     * - array items.$ref
     * - allOf single $ref (optional edge case)
     */
    private void annotateElementCollectionsOnFields(
        final ObjectNode definitionsNode, final Map<String, ModelType> kindByName) {
        if (definitionsNode == null) {
            return;
        }

        definitionsNode
            .fieldNames()
            .forEachRemaining(
                defName -> {
                    final JsonNode defNode = definitionsNode.get(defName);
                    if (!(defNode instanceof ObjectNode)) {
                        return;
                    }

                    final ObjectNode defObj = (ObjectNode) defNode;
                    final JsonNode propsNode = defObj.get("properties");
                    if (!(propsNode instanceof ObjectNode)) {
                        return;
                    }

                    final ObjectNode propsObj = (ObjectNode) propsNode;
                    propsObj
                        .fieldNames()
                        .forEachRemaining(
                            propName -> {
                                final JsonNode propSchema =
                                    propsObj.get(propName);
                                if (!(propSchema instanceof ObjectNode)) {
                                    return;
                                }
                                final ObjectNode propObj =
                                    (ObjectNode) propSchema;

                                // Case A: direct $ref
                                if (propObj.has("$ref")
                                    && propObj.get("$ref").isTextual()) {
                                    if (refTargetsEmbeddable(
                                        propObj.get("$ref").asText(),
                                        kindByName)) {
                                        propObj.put("x-element-collection", true);
                                        return;
                                    }
                                }

                                // Case B: array items.$ref
                                if ("array"
                                    .equals(asText(propObj.get("type")))) {
                                    final JsonNode items = propObj.get("items");
                                    if (items instanceof ObjectNode) {
                                        final ObjectNode itemsObj =
                                            (ObjectNode) items;
                                        if (itemsObj.has("$ref")
                                            && itemsObj.get("$ref")
                                            .isTextual()) {
                                            if (refTargetsEmbeddable(
                                                itemsObj.get("$ref").asText(),
                                                kindByName)) {
                                                propObj.put(
                                                    "x-element-collection",
                                                    true);
                                                return;
                                            }
                                        }
                                        // Case B2: array items allOf single $ref
                                        if (itemsObj.has("allOf")
                                            && itemsObj
                                            .get("allOf")
                                            .isArray()) {
                                            final var arr =
                                                (ArrayNode)
                                                    itemsObj.get("allOf");
                                            if (arr.size() == 1
                                                && arr.get(0).has("$ref")) {
                                                if (refTargetsEmbeddable(
                                                    arr.get(0)
                                                        .get("$ref")
                                                        .asText(),
                                                    kindByName)) {
                                                    propObj.put(
                                                        "x-element-collection",
                                                        true);
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                }

                                // Case C: allOf with single $ref
                                if (propObj.has("allOf")
                                    && propObj.get("allOf").isArray()) {
                                    final var arr =
                                        (ArrayNode) propObj.get("allOf");
                                    if (arr.size() == 1 && arr.get(0).has("$ref")) {
                                        if (refTargetsEmbeddable(
                                            arr.get(0).get("$ref").asText(),
                                            kindByName)) {
                                            propObj.put(
                                                "x-element-collection", true);
                                        }
                                    }
                                }
                            });
                });
    }

    private boolean refTargetsEmbeddable(
        final String refText, final Map<String, ModelType> kindByName) {
        final String prefix = "#/components/schemas/";
        if (!refText.startsWith(prefix)) {
            return false;
        }
        String name = refText.substring(prefix.length());
        // Strip "Entity" suffix (we append that during rewrite)
        if (name.endsWith("Entity")) {
            name = name.substring(0, name.length() - "Entity".length());
        }
        return kindByName.getOrDefault(name, ModelType.ISOLATED) == ModelType.EMBEDDABLE;
    }

    private String asText(JsonNode n) {
        return (n != null && n.isTextual()) ? n.asText() : null;
    }

    /**
     * Walks the entire schema tree and rewrites $ref targets according to classifier results.
     */
    private void rewriteRefsByKind(final JsonNode node, final Map<String, ModelType> kindByName) {
        if (Objects.isNull(node)) {
            return;
        }

        if (node.isObject()) {
            final var obj = (ObjectNode) node;

            if (obj.has("$ref") && obj.get("$ref").isTextual()) {
                final var refText = obj.get("$ref").asText();
                final var prefix = "#/components/schemas/";
                if (refText.startsWith(prefix)) {
                    final var name = refText.substring(prefix.length());
                    final var type = kindByName.getOrDefault(name, ModelType.ISOLATED);

                    final var newTarget =
                        (type == ModelType.ENTITY
                            || type == ModelType.ISOLATED
                            || type == ModelType.EMBEDDABLE)
                            ? (name + "Entity")
                            : name;

                    obj.put("$ref", prefix + newTarget);
                }
            }

            obj.fields()
                .forEachRemaining(
                    e -> {
                        final var k = e.getKey();
                        if (!"$ref".equals(k)) {
                            rewriteRefsByKind(e.getValue(), kindByName);
                        }
                    });
        } else if (node.isArray()) {
            node.forEach(child -> rewriteRefsByKind(child, kindByName));
        }
    }

    private ObjectNode ensureObjectNode(final JsonNode node) {
        if (node instanceof ObjectNode) {
            return (ObjectNode) node;
        }
        final var wrapper = objectMapper.createObjectNode();
        final var arr = objectMapper.createArrayNode();
        arr.add(node);
        wrapper.set("allOf", arr);
        return wrapper;
    }

    private JsonNode deepClone(final JsonNode node) {
        try {
            return objectMapper.readTree(node.toString());
        } catch (Exception e) {
            return node;
        }
    }
}
