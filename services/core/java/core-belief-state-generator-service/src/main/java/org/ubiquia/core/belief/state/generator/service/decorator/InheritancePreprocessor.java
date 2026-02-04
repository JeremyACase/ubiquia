package org.ubiquia.core.belief.state.generator.service.decorator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InheritancePreprocessor {

    private static final Logger logger = LoggerFactory.getLogger(UbiquiaModelInjector.class);

    private static final String REF_VALUE = "#/definitions/AbstractDomainModel";

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Accepts a JSON schema as a string, modifies it, and returns the result as a string.
     *
     * @param jsonSchema The input JSON schema as a string
     * @return The transformed JSON schema as a string
     * @throws IOException If JSON parsing or writing fails
     */
    public String appendInheritance(final String jsonSchema) throws IOException {

        logger.debug("Appending inheritance to: {}...", jsonSchema);
        var root = this.objectMapper.readTree(jsonSchema);
        var transformed = this.transform(root);
        var modified = this
            .objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(transformed);
        logger.debug("...modified to: {}", modified);
        return jsonSchema;
    }

    /**
     * Transforms the supplied JSON Schema root node in-place where applicable.
     */
    private JsonNode transform(JsonNode root) {
        if (!root.isObject()) return root;

        var rootObj = ((ObjectNode) root).deepCopy();

        // Handle Draft-04 style "definitions"
        if (rootObj.has("definitions")) {
            this.processContainer((ObjectNode) rootObj.get("definitions"));
        }

        // Handle OpenAPI-style "components.schemas"
        if (rootObj.has("components") && rootObj.get("components").has("schemas")) {
            this.processContainer((ObjectNode) rootObj.get("components").get("schemas"));
        }

        return rootObj;
    }

    /**
     * Iterate over each definition/schema in the container and add the allOf reference when needed.
     */
    private void processContainer(ObjectNode container) {
        var fields = container.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            if (!entry.getValue().isObject()) continue;

            var schema = (ObjectNode) entry.getValue();

            // Skip enums and existing allOfs
            if (schema.has("enum") || schema.has("allOf")) {
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

