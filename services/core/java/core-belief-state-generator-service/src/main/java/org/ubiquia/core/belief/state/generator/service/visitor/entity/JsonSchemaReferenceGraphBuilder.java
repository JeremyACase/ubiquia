package org.ubiquia.core.belief.state.generator.service.visitor.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.belief.state.generator.model.json.schema.JsonSchemaGraph;

@Service
public class JsonSchemaReferenceGraphBuilder {

    @Autowired
    private JsonSchemaNodeUtils utils;

    public JsonSchemaGraph build(final ObjectNode defsRoot) {
        var result = new JsonSchemaGraph(Map.of(), Map.of(), Set.of());

        if (Objects.nonNull(defsRoot)) {
            final var defMap = fieldMap(defsRoot);
            final var names = defMap.keySet();

            // outRefs: ignore refs to enums (they don't count as object refs)
            final var outRefs = new LinkedHashMap<String, Set<String>>();
            for (var e : defMap.entrySet()) {
                final var name = e.getKey();
                final var def = e.getValue();

                if (Objects.isNull(def) || utils.isEnumDefinition(def)) {
                    outRefs.put(name, Set.of());
                } else {
                    final var outs = new HashSet<String>();
                    collectObjectRefs(def, outs, defsRoot, names);
                    outRefs.put(name, outs);
                }
            }

            // inRefs
            final var inRefs = new LinkedHashMap<String, Set<String>>();
            for (var n : names) {
                inRefs.put(n, new LinkedHashSet<>());
            }
            outRefs.forEach((src, outs) -> outs.forEach(dst -> inRefs.get(dst).add(src)));

            result = new JsonSchemaGraph(outRefs, inRefs, names);
        }

        return result;
    }

    private Map<String, JsonNode> fieldMap(final ObjectNode obj) {
        final var map = new LinkedHashMap<String, JsonNode>();

        if (Objects.nonNull(obj)) {
            final var it = obj.fieldNames();
            while (it.hasNext()) {
                final var k = it.next();
                map.put(k, obj.get(k));
            }
        }

        return map;
    }

    private void collectObjectRefs(
        final JsonNode node,
        final Set<String> out,
        final ObjectNode defsRoot,
        final Set<String> knownNames) {

        if (Objects.isNull(node)) {
            return;
        }

        if (node.isObject()) {
            // $ref
            if (node.has("$ref") && node.get("$ref").isTextual()) {
                final var local = utils.extractLocalRefName(node.get("$ref").asText());
                if (Objects.nonNull(local) && knownNames.contains(local)) {
                    // only count non-enum targets
                    final var target = defsRoot.get(local);
                    if (!utils.isEnumDefinition(target)) {
                        out.add(local);
                    }
                }
            }

            // walk typical containers/combinators
            node.fields().forEachRemaining(e -> {
                final var k = e.getKey();
                final var v = e.getValue();

                if ("properties".equals(k) && v.isObject()) {
                    v.fields().forEachRemaining(p ->
                        collectObjectRefs(p.getValue(), out, defsRoot, knownNames)
                    );
                } else if ("items".equals(k)) {
                    collectObjectRefs(v, out, defsRoot, knownNames);
                } else if ("allOf".equals(k) || "anyOf".equals(k) || "oneOf".equals(k)) {
                    if (v.isArray()) {
                        v.forEach(ch -> collectObjectRefs(ch, out, defsRoot, knownNames));
                    }
                } else if ("additionalProperties".equals(k) && v.isObject()) {
                    collectObjectRefs(v, out, defsRoot, knownNames);
                } else if (!"$ref".equals(k)) {
                    collectObjectRefs(v, out, defsRoot, knownNames);
                }
            });
        } else if (node.isArray()) {
            node.forEach(ch -> collectObjectRefs(ch, out, defsRoot, knownNames));
        }
    }
}
