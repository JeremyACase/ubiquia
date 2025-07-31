package org.ubiquia.core.belief.state.generator.service.decorator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EnumNormalizer {

    @Autowired
    private ObjectMapper objectMapper;

    public String normalizeEnums(String rawSchemaJson) throws IOException {
        var root = this.objectMapper.readTree(rawSchemaJson);
        var jsonSchemaNode = root.get("jsonSchema");

        var normalizedResult = rawSchemaJson;
        if (jsonSchemaNode instanceof ObjectNode jsonSchemaObject) {
            var definitions = jsonSchemaObject.with("definitions");
            var enumToDefName = new HashMap<String, String>();

            for (var defName : Lists.newArrayList(definitions.fieldNames())) {
                var defNode = definitions.get(defName);
                if (defNode instanceof ObjectNode def
                    && "object".equals(def.path("type").asText())) {

                    var propsNode = def.get("properties");
                    if (propsNode instanceof ObjectNode props) {
                        for (var propName : Lists.newArrayList(props.fieldNames())) {
                            var propNode = props.get(propName);
                            if (propNode instanceof ObjectNode prop
                                && prop.has("enum") && !prop.has("$ref")) {

                                var enumArray = (ArrayNode) prop.get("enum");
                                var enumValues = new ArrayList<String>();
                                enumArray.forEach(v -> enumValues.add(v.asText()));
                                Collections.sort(enumValues);
                                var enumKey = String.join("|", enumValues);

                                var defEnumName = enumToDefName.computeIfAbsent(enumKey, k -> {
                                    var hash = DigestUtils.sha256Hex(k).substring(0, 8);
                                    var enumDefName = "Enum_" + hash;
                                    var newEnum = this.objectMapper.createObjectNode();
                                    newEnum.put("type", "string");
                                    newEnum.set("enum", enumArray);
                                    definitions.set(enumDefName, newEnum);
                                    return enumDefName;
                                });

                                var refNode = this.objectMapper.createObjectNode();
                                refNode.put("$ref", "#/definitions/" + defEnumName);
                                props.set(propName, refNode);
                            }
                        }
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
}
