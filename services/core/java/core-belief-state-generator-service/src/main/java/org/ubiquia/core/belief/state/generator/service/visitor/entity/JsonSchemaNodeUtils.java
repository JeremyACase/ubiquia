package org.ubiquia.core.belief.state.generator.service.visitor.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Utility methods for inspecting JSON Schema nodes. */
@Service
public class JsonSchemaNodeUtils {

    /** Returns true if {@code node} represents a string enum definition. */
    public boolean isEnumDefinition(final JsonNode node) {
        var result = false;

        if (Objects.nonNull(node)
            && node.has("type")
            && "string".equals(node.get("type").asText())
            && node.has("enum")
            && node.get("enum").isArray()) {
            result = true;
        }

        return result;
    }

    /** Extracts the local definition name from a {@code $ref} value, or returns null. */
    public String extractLocalRefName(final String ref) {
        var result = (String) null;

        if (Objects.nonNull(ref)) {
            final var p1 = "#/definitions/";
            final var p2 = "#/components/schemas/";

            if (ref.startsWith(p1)) {
                result = ref.substring(p1.length());
            } else if (ref.startsWith(p2)) {
                result = ref.substring(p2.length());
            }
        }

        return result;
    }
}
