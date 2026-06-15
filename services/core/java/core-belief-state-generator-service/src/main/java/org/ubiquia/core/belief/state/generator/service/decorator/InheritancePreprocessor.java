package org.ubiquia.core.belief.state.generator.service.decorator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Adds {@code allOf} inheritance references to every definition that does not already
 * declare one, pointing to {@code #/definitions/AbstractDomainModel}.
 */
@Order(3)
@Service
public class InheritancePreprocessor implements SchemaTransformer {

    private static final Logger logger = LoggerFactory.getLogger(InheritancePreprocessor.class);

    private static final String REF_VALUE = "#/definitions/AbstractDomainModel";

    // These Ubiquia infrastructure models must not be given an allOf self-reference
    // (AbstractDomainModel) or a circular inheritance chain (KeyValuePair is embeddable,
    // not an entity, and AbstractDomainModel already references it via its tags field).
    private static final String BASE_MODEL_NAME = "AbstractDomainModel";
    private static final String KEY_VALUE_PAIR_NAME = "KeyValuePair";

    @Autowired
    private ObjectMapper objectMapper;

    /** {@inheritDoc} */
    @Override
    public String transform(final String schema) throws IOException {

        logger.debug("Appending inheritance to: {}...", schema);
        var root = this.objectMapper.readTree(schema);
        var transformed = this.applyTransformations(root);
        var modified = this
            .objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(transformed);
        logger.debug("...modified to: {}", modified);
        return modified;
    }

    /**
     * Transforms the supplied JSON Schema root node in-place where applicable.
     *
     * @param root the root JSON node to transform
     * @return the transformed node
     */
    private JsonNode applyTransformations(final JsonNode root) {
        if (!root.isObject()) {
            return root;
        }

        var rootObj = ((ObjectNode) root).deepCopy();

        if (rootObj.has("definitions")) {
            this.processContainer((ObjectNode) rootObj.get("definitions"));
        }

        if (rootObj.has("components") && rootObj.get("components").has("schemas")) {
            this.processContainer((ObjectNode) rootObj.get("components").get("schemas"));
        }

        return rootObj;
    }

    /**
     * Iterates over each definition in the container and adds an allOf reference when needed.
     *
     * @param container the definitions or schemas container node
     */
    private void processContainer(final ObjectNode container) {
        var fields = container.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            if (!entry.getValue().isObject()) {
                continue;
            }

            var schema = (ObjectNode) entry.getValue();

            if (schema.has("enum") || schema.has("allOf")
                || BASE_MODEL_NAME.equals(entry.getKey())
                || KEY_VALUE_PAIR_NAME.equals(entry.getKey())) {
                continue;
            }

            var allOf = objectMapper.createArrayNode();
            var refNode = objectMapper.createObjectNode();
            refNode.put("$ref", REF_VALUE);
            allOf.add(refNode);

            schema.set("allOf", allOf);
        }
    }
}
