package org.ubiquia.core.belief.state.generator.service.visitor.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JsonSchemaDefinitionIndex {


    public ObjectNode extractDefinitionsRoot(final JsonNode root) {

        ObjectNode result = null;
        if (Objects.nonNull(root) && root.isObject()) {
            var definitionsNode = root.get("definitions");
            if (Objects.nonNull(definitionsNode) && definitionsNode.isObject()) {
                result = (ObjectNode) definitionsNode;
            } else {
                var componentsNode = root.get("components");
                if (Objects.nonNull(componentsNode) && componentsNode.isObject()) {
                    var schemasNode = componentsNode.get("schemas");
                    if (Objects.nonNull(schemasNode) && schemasNode.isObject()) {
                        result = (ObjectNode) schemasNode;
                    }
                }
            }
        }

        return result;
    }
}