package org.ubiquia.core.belief.state.generator.service.decorator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UbiquiaModelInjector {

    private static final Logger logger = LoggerFactory.getLogger(UbiquiaModelInjector.class);

    @Autowired
    private ObjectMapper objectMapper;

    public String appendAclModels(final String jsonSchema) throws JsonProcessingException {

        logger.debug("Appending Ubiquia models to: {}...", jsonSchema);

        // 1. Parse the input
        var root = (ObjectNode) this.objectMapper.readTree(jsonSchema);

        // 2. Ensure /definitions exists
        var defs = (ObjectNode) root.get("definitions");
        if (defs == null) {
            defs = this.objectMapper.createObjectNode();
            root.set("definitions", defs);
        }

        // 3. Create KeyValuePair definition
        var kvPair = this.objectMapper.createObjectNode()
            .put("type", "object")
            .put("additionalProperties", false);

        var kvProps = this.objectMapper.createObjectNode();
        kvProps.set("key",   stringOrNull("Tag key"));
        kvProps.set("value", stringOrNull("Tag value"));
        kvPair.set("properties", kvProps);
        kvPair.set("required", this.objectMapper.createArrayNode()); // add "key","value" if required

        // 4. Create AbstractAclEntity definition
        var acl = this.objectMapper.createObjectNode()
            .put("type", "object")
            .put("additionalProperties", false);

        var aclProps = this.objectMapper.createObjectNode();
        aclProps.set("id",        uuid("Database-generated UUID"));
        aclProps.set("createdAt", timestamp("Creation timestamp", true));
        aclProps.set("updatedAt", timestamp("Last update timestamp", true));
        aclProps.set("tags", tagsArray());
        aclProps.set("modelType", stringOrNull("Transient helper for type introspection"));
        acl.set("properties", aclProps);
        acl.set("required", this.objectMapper.createArrayNode()); // e.g. ["id"] if you need it

        // 5. Insert if not already present
        defs.putIfAbsent("KeyValuePair", kvPair);
        defs.putIfAbsent("AbstractAclModel", acl);

        var preprocssed = this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        logger.debug("...appended Ubiquia models: {}.", preprocssed);

        return preprocssed;
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
