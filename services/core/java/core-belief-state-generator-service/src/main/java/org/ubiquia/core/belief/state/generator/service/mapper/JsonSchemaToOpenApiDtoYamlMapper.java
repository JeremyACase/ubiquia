package org.ubiquia.core.belief.state.generator.service.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.ubiquia.core.belief.state.generator.model.json.schema.ModelType;

/**
 * Produces an OpenAPI 3.0 YAML document targeting DTO generation.
 *
 * <p>Definition names are kept as-is. EMBEDDABLE definitions receive the
 * {@code x-embeddable: true} vendor extension so the DTO generator can
 * emit the appropriate annotations.
 */
@Service
public class JsonSchemaToOpenApiDtoYamlMapper extends AbstractJsonSchemaToOpenApiYamlMapper {

    @Override
    protected void visitDefinition(
        final String defName,
        final JsonNode defNode,
        final ModelType kind,
        final ObjectNode schemasNode) {

        if (kind == ModelType.EMBEDDABLE) {
            this.ensureObjectNode(defNode).put("x-embeddable", true);
        }
        schemasNode.set(defName, defNode);
    }
}
