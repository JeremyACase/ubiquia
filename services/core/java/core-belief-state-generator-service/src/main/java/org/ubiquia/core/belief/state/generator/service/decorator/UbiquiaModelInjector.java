package org.ubiquia.core.belief.state.generator.service.decorator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Injects Ubiquia domain model definitions ({@code AbstractDomainModel}, {@code KeyValuePair})
 * into the top-level {@code definitions} block of the JSON schema.
 */
@Order(2)
@Service
public class UbiquiaModelInjector implements SchemaTransformer {

    private static final Logger logger = LoggerFactory.getLogger(UbiquiaModelInjector.class);

    @Autowired
    private ObjectMapper objectMapper;

    /** {@inheritDoc} */
    @Override
    public String transform(final String schema) throws IOException {

        logger.debug("Appending Ubiquia models to: {}...", schema);

        var node = objectMapper.readTree(schema);

        if (!node.isObject()) {
            throw new IllegalArgumentException(
                "Expected JSON object for schema, but got: "
                    + node.getNodeType()
                    + " value=" + node
            );
        }

        var root = (ObjectNode) node;

        var defs = (ObjectNode) root.get("definitions");
        if (Objects.isNull(defs)) {
            defs = this.objectMapper.createObjectNode();
            root.set("definitions", defs);
        }

        var kvPair = this.objectMapper.createObjectNode()
            .put("type", "object")
            .put("additionalProperties", false);

        var kvProps = this.objectMapper.createObjectNode();
        kvProps.set("key", stringOrNull("Tag key"));
        kvProps.set("value", stringOrNull("Tag value"));
        kvPair.set("properties", kvProps);
        kvPair.set("required", this.objectMapper.createArrayNode());

        final var domainSchema = this.objectMapper.createObjectNode()
            .put("type", "object")
            .put("additionalProperties", false);

        var domainProperties = this.objectMapper.createObjectNode();
        domainProperties.set("id", uuid("Database-generated UUID"));
        domainProperties.set("createdAt", timestamp("Creation timestamp", true));
        domainProperties.set("updatedAt", timestamp("Last update timestamp", true));
        domainProperties.set("tags", tagsArray());
        domainSchema.set("properties", domainProperties);
        domainSchema.set("required", this.objectMapper.createArrayNode());

        defs.putIfAbsent("KeyValuePair", kvPair);
        defs.putIfAbsent("AbstractDomainModel", domainSchema);

        var preprocessed = this.objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(root);
        logger.debug("...appended Ubiquia models: {}.", preprocessed);

        return preprocessed;
    }

    private ObjectNode stringOrNull(final String desc) {
        var node = this.objectMapper.createObjectNode();
        node.put("type", "string");
        node.put("description", desc);
        node.put("nullable", true);
        return node;
    }

    private ObjectNode uuid(final String desc) {
        var n = this.stringOrNull(desc);
        n.put("format", "uuid");
        return n;
    }

    private ObjectNode timestamp(final String desc, final Boolean readOnly) {
        var n = this.stringOrNull(desc);
        n.put("format", "date-time");
        n.put("readOnly", readOnly);
        return n;
    }

    private ObjectNode tagsArray() {
        var n = this.objectMapper.createObjectNode();
        n.put("type", "array");
        n.put("nullable", true);
        n.put("description", "User-defined metadata tags");
        n.put("uniqueItems", true);
        n.set("items", this.objectMapper.createObjectNode()
            .put("$ref", "#/definitions/KeyValuePair"));
        return n;
    }
}
