package org.ubiquia.core.belief.state.generator.service.visitor.entity;

import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class GraphCycleDetector {

    /**
     * Returns all nodes that participate in at least one directed cycle.
     */
    public Set<String> nodesInAnyCycle(
        final Set<String> nodes,
        final Map<String, Set<String>> outRefs) {

        var result = new HashSet<String>();

        if (Objects.nonNull(nodes) && Objects.nonNull(outRefs)) {
            final var color = new HashMap<String, Color>();
            for (var n : nodes) {
                color.put(n, Color.WHITE);
            }

            final var stack = new ArrayDeque<String>();

            for (var n : nodes) {
                if (color.get(n) == Color.WHITE) {
                    dfs(n, outRefs, color, stack, result);
                }
            }
        }

        return result;
    }

    private void dfs(
        final String u,
        final Map<String, Set<String>> outRefs,
        final Map<String, Color> color,
        final Deque<String> stack,
        final Set<String> inCycle) {

        color.put(u, Color.GRAY);
        stack.push(u);

        for (var v : outRefs.getOrDefault(u, Set.of())) {
            var cv = color.getOrDefault(v, Color.WHITE);
            if (cv == Color.WHITE) {
                dfs(v, outRefs, color, stack, inCycle);
            } else if (cv == Color.GRAY) {
                // found a back-edge; mark cycle path from v to u
                for (var x : stack) {
                    inCycle.add(x);
                    if (x.equals(v)) {
                        break;
                    }
                }
            }
        }

        stack.pop();
        color.put(u, Color.BLACK);
    }

    /**
     * Direct mutual back-reference check (A->B && B->A)
     */
    public boolean hasDirectMutualRef(
        final String name,
        final Map<String, Set<String>> outRefs) {

        var result = false;

        if (Objects.nonNull(name) && Objects.nonNull(outRefs)) {
            final var outs = outRefs.getOrDefault(name, Set.of());
            for (var b : outs) {
                if (outRefs.getOrDefault(b, Set.of()).contains(name)) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    private enum Color { WHITE, GRAY, BLACK }
}
