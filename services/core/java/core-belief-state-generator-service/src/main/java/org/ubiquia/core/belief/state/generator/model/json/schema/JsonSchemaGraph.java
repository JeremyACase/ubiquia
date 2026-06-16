package org.ubiquia.core.belief.state.generator.model.json.schema;

import java.util.Map;
import java.util.Set;

/** Immutable graph capturing outgoing and incoming JSON Schema definition references. */
public class JsonSchemaGraph {
    public Map<String, Set<String>> outRefs;
    public Map<String, Set<String>> inRefs;
    public Set<String> allNodes;

    /** Constructs a graph with the given reference maps and node set. */
    public JsonSchemaGraph(
        final Map<String, Set<String>> outRefs,
        final Map<String, Set<String>> inRefs,
        final Set<String> allNodes) {

        this.outRefs = outRefs;
        this.inRefs = inRefs;
        this.allNodes = allNodes;
    }
}