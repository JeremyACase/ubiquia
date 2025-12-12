package org.ubiquia.core.flow.service.builder.node;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.embeddable.KeyValuePair;
import org.ubiquia.core.flow.component.node.AbstractNode;

@Service
public class NodeTagBuilder {

    /**
     * Build tags for a provided adapter.
     *
     * @param node The adapter to build tags for.
     * @return The list of tags.
     */
    public List<KeyValuePair> buildNodeTags(final AbstractNode node) {

        var tags = new ArrayList<KeyValuePair>();

        var context = node.getNodeContext();
        tags.add(new KeyValuePair(
            "nodeType",
            context.getNodeType().toString()));
        tags.add(new KeyValuePair(
            "graphName",
            context.getGraph().getName()));
        tags.add(new KeyValuePair(
            "nodeName",
            context.getNodeName()));

        if (Objects.nonNull(context.getComponent())) {
            var kvp = new KeyValuePair(
                "componentName",
                context.getComponent().getName());
            tags.add(kvp);
        }

        return tags;
    }
}
