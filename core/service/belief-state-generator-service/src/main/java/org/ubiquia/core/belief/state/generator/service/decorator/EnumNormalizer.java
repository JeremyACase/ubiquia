package org.ubiquia.core.belief.state.generator.service.decorator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EnumNormalizer {

    @Autowired
    private ObjectMapper objectMapper;

    public String normalizeEnums(String rawSchemaJson) throws IOException {
        var normalizedResult = rawSchemaJson;

        var root = this.objectMapper.readTree(rawSchemaJson);
        var jsonSchemaNode = root.get("jsonSchema");

        if (jsonSchemaNode instanceof ObjectNode jsonSchemaObject) {
            var definitions = jsonSchemaObject.with("definitions");
            var enumToDefName = new HashMap<String, String>();

            if (jsonSchemaObject.has("properties")) {
                var topLevelPropsNode = jsonSchemaObject.get("properties");
                if (topLevelPropsNode instanceof ObjectNode topLevelProps) {
                    normalizeEnumsInProperties(topLevelProps, definitions, enumToDefName);
                }
            }

            var defNames = Lists.newArrayList(definitions.fieldNames());
            for (String defName : defNames) {
                var defNode = definitions.get(defName);

                var isObjectNode = defNode instanceof ObjectNode;
                var type = isObjectNode ? defNode.path("type").asText() : null;

                if (isObjectNode && "object".equals(type)) {
                    var def = (ObjectNode) defNode;
                    var propsNode = def.get("properties");

                    if (propsNode instanceof ObjectNode props) {
                        this.normalizeEnumsInProperties(
                            props,
                            definitions,
                            enumToDefName);
                    }
                }
            }

            normalizedResult = this
                .objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(root);
        }

        return normalizedResult;
    }

    private void normalizeEnumsInProperties(
        ObjectNode props,
        ObjectNode definitions,
        Map<String, String> enumToDefName
    ) {
        var propNames = Lists.newArrayList(props.fieldNames());

        for (var propName : propNames) {
            var propNode = props.get(propName);

            var isObjectNode = propNode instanceof ObjectNode;
            var hasEnum = isObjectNode && (propNode).has("enum");
            var hasRef = isObjectNode && (propNode).has("$ref");

            if (isObjectNode && hasEnum && !hasRef) {
                var prop = (ObjectNode) propNode;
                var enumArray = (ArrayNode) prop.get("enum");

                var enumValues = new ArrayList<String>();
                enumArray.forEach(v -> enumValues.add(v.asText()));
                Collections.sort(enumValues);

                var enumKey = String.join("|", enumValues);

                String defEnumName = null;
                if (enumToDefName.containsKey(enumKey)) {
                    defEnumName = enumToDefName.get(enumKey);
                } else {
                    String hash = DigestUtils.sha256Hex(enumKey).substring(0, 8);
                    defEnumName = "Enum_" + hash;

                    var newEnum = this.objectMapper.createObjectNode();
                    newEnum.put("type", "string");
                    newEnum.set("enum", enumArray);

                    definitions.set(defEnumName, newEnum);
                    enumToDefName.put(enumKey, defEnumName);
                }

                var refNode = this.objectMapper.createObjectNode();
                refNode.put("$ref", "#/definitions/" + defEnumName);
                props.set(propName, refNode);
            }
        }
    }
}
