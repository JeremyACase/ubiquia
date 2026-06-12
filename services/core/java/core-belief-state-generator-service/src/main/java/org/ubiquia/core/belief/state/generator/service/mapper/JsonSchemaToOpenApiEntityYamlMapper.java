package org.ubiquia.core.belief.state.generator.service.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.ubiquia.core.belief.state.generator.model.json.schema.ModelType;

/**
 * Produces an OpenAPI 3.0 YAML document targeting JPA entity generation.
 *
 * <p>Non-enum definitions are suffixed with {@code Entity}. Before iteration, {@code $ref}
 * targets are rewritten to include the suffix, and fields whose referenced type is EMBEDDABLE
 * receive {@code x-element-collection: true}.
 */
@Service
public class JsonSchemaToOpenApiEntityYamlMapper extends AbstractJsonSchemaToOpenApiYamlMapper {

    private static final String SCHEMA_PREFIX = "#/components/schemas/";

    @Override
    protected void preprocessDefinitions(
        final ObjectNode mainSchema,
        final Map<String, ModelType> kindByName) {

        this.rewriteRefsByKind(mainSchema, kindByName);

        if (mainSchema.has("definitions")) {
            this.annotateElementCollectionsOnFields(
                (ObjectNode) mainSchema.get("definitions"), kindByName);
        }
    }

    @Override
    protected void visitDefinition(
        final String defName,
        final JsonNode defNode,
        final ModelType kind,
        final ObjectNode schemasNode) {

        switch (kind) {
            case ENUM:
                schemasNode.set(defName, defNode);
                break;
            case EMBEDDABLE:
                this.ensureObjectNode(defNode).put("x-embeddable", true);
                schemasNode.set(defName + "Entity", defNode);
                break;
            default:
                schemasNode.set(defName + "Entity", defNode);
                break;
        }
    }

    /**
     * Walks the entire schema tree and rewrites {@code $ref} targets to include the
     * {@code Entity} suffix for ENTITY, ISOLATED, and EMBEDDABLE definitions.
     *
     * @param node       the root node to walk (mutated in-place)
     * @param kindByName definition name to classified {@link ModelType}
     */
    private void rewriteRefsByKind(
        final JsonNode node,
        final Map<String, ModelType> kindByName) {

        if (Objects.isNull(node)) {
            return;
        }

        if (node.isObject()) {
            var obj = (ObjectNode) node;

            if (obj.has("$ref") && obj.get("$ref").isTextual()) {
                var refText = obj.get("$ref").asText();
                if (refText.startsWith(SCHEMA_PREFIX)) {
                    var name = refText.substring(SCHEMA_PREFIX.length());
                    var type = kindByName.getOrDefault(name, ModelType.ISOLATED);
                    var newTarget = (type == ModelType.ENTITY
                        || type == ModelType.ISOLATED
                        || type == ModelType.EMBEDDABLE)
                        ? name + "Entity"
                        : name;
                    obj.put("$ref", SCHEMA_PREFIX + newTarget);
                }
            }

            obj.fields().forEachRemaining(e -> {
                if (!"$ref".equals(e.getKey())) {
                    this.rewriteRefsByKind(e.getValue(), kindByName);
                }
            });

        } else if (node.isArray()) {
            node.forEach(child -> this.rewriteRefsByKind(child, kindByName));
        }
    }

    /**
     * Marks fields that reference EMBEDDABLE types with {@code x-element-collection: true}.
     * Handles direct {@code $ref}, array {@code items.$ref}, and {@code allOf} single-ref cases.
     *
     * @param definitionsNode the definitions container node
     * @param kindByName      definition name to classified {@link ModelType}
     */
    private void annotateElementCollectionsOnFields(
        final ObjectNode definitionsNode,
        final Map<String, ModelType> kindByName) {

        if (Objects.isNull(definitionsNode)) {
            return;
        }

        definitionsNode.fieldNames().forEachRemaining(defName -> {
            var defNode = definitionsNode.get(defName);
            if (!(defNode instanceof ObjectNode defObj)) {
                return;
            }

            var propsNode = defObj.get("properties");
            if (!(propsNode instanceof ObjectNode propsObj)) {
                return;
            }

            propsObj.fieldNames().forEachRemaining(propName -> {
                var propSchema = propsObj.get(propName);
                if (!(propSchema instanceof ObjectNode propObj)) {
                    return;
                }

                // Case A: direct $ref
                if (propObj.has("$ref") && propObj.get("$ref").isTextual()) {
                    if (this.refTargetsEmbeddable(propObj.get("$ref").asText(), kindByName)) {
                        propObj.put("x-element-collection", true);
                        return;
                    }
                }

                // Case B: array items.$ref or items.allOf[$ref]
                if ("array".equals(asText(propObj.get("type")))) {
                    var items = propObj.get("items");
                    if (items instanceof ObjectNode itemsObj) {
                        if (itemsObj.has("$ref") && itemsObj.get("$ref").isTextual()) {
                            if (this.refTargetsEmbeddable(
                                itemsObj.get("$ref").asText(), kindByName)) {
                                propObj.put("x-element-collection", true);
                                return;
                            }
                        }
                        if (itemsObj.has("allOf") && itemsObj.get("allOf").isArray()) {
                            var arr = (ArrayNode) itemsObj.get("allOf");
                            if (arr.size() == 1 && arr.get(0).has("$ref")) {
                                if (this.refTargetsEmbeddable(
                                    arr.get(0).get("$ref").asText(), kindByName)) {
                                    propObj.put("x-element-collection", true);
                                    return;
                                }
                            }
                        }
                    }
                }

                // Case C: allOf with single $ref
                if (propObj.has("allOf") && propObj.get("allOf").isArray()) {
                    var arr = (ArrayNode) propObj.get("allOf");
                    if (arr.size() == 1 && arr.get(0).has("$ref")) {
                        if (this.refTargetsEmbeddable(
                            arr.get(0).get("$ref").asText(), kindByName)) {
                            propObj.put("x-element-collection", true);
                        }
                    }
                }
            });
        });
    }

    private boolean refTargetsEmbeddable(
        final String refText,
        final Map<String, ModelType> kindByName) {

        if (!refText.startsWith(SCHEMA_PREFIX)) {
            return false;
        }
        var name = refText.substring(SCHEMA_PREFIX.length());
        if (name.endsWith("Entity")) {
            name = name.substring(0, name.length() - "Entity".length());
        }
        return kindByName.getOrDefault(name, ModelType.ISOLATED) == ModelType.EMBEDDABLE;
    }

    private String asText(final JsonNode n) {
        return (Objects.nonNull(n) && n.isTextual()) ? n.asText() : null;
    }
}
