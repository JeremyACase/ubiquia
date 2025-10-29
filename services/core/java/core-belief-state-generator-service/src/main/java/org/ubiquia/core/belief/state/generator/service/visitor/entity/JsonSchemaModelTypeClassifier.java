package org.ubiquia.core.belief.state.generator.service.visitor.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.belief.state.generator.model.json.schema.ModelType;

@Service
public class JsonSchemaModelTypeClassifier {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JsonSchemaDefinitionIndex definitionIndex;

    @Autowired
    private JsonSchemaReferenceGraphBuilder graphBuilder;

    @Autowired
    private GraphCycleDetector cycleDetector;

    @Autowired
    private JsonSchemaNodeUtils utils;

    public Map<String, ModelType> classify(final String jsonSchemaString) {
        Map<String, ModelType> result = Map.of();

        try {
            final var root = objectMapper.readTree(jsonSchemaString);
            final var defsRoot = definitionIndex.extractDefinitionsRoot(root);

            if (Objects.nonNull(defsRoot)) {
                final var g = graphBuilder.build(defsRoot);
                final var cycleNodes = cycleDetector.nodesInAnyCycle(g.allNodes, g.outRefs);

                final var computed = new LinkedHashMap<String, ModelType>();
                for (var name : g.allNodes) {
                    final var def = defsRoot.get(name);

                    if (utils.isEnumDefinition(def)) {
                        computed.put(name, ModelType.ENUM);
                        continue;
                    }

                    final var out = g.outRefs.getOrDefault(name, Set.of()).size();
                    final var in  = g.inRefs.getOrDefault(name, Set.of()).size();

                    if (cycleNodes.contains(name) || cycleDetector.hasDirectMutualRef(name, g.outRefs)) {
                        computed.put(name, ModelType.ENTITY);       // bidirectional path/cycle
                    } else if (out > 0) {
                        computed.put(name, ModelType.ENTITY);       // unidirectional: has outgoing refs
                    } else if (in > 0) {
                        computed.put(name, ModelType.EMBEDDABLE);   // referenced by others, references none
                    } else {
                        computed.put(name, ModelType.ISOLATED);     // neither in nor out
                    }
                }

                result = computed;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to classify JSON Schema definitions", e);
        }

        return result;
    }
}
