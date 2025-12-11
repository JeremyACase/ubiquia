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
     * @param adapter The adapter to build tags for.
     * @return The list of tags.
     */
    public List<KeyValuePair> buildAdapterTags(final AbstractNode adapter) {

        var tags = new ArrayList<KeyValuePair>();

        var context = adapter.getNodeContext();
        tags.add(new KeyValuePair("nodeType", context.getNodeType().toString()));
        tags.add(new KeyValuePair("graphName", context.getGraphName()));
        tags.add(new KeyValuePair("nodeName", context.getNodeName()));

        if (Objects.nonNull(context.getComponentName())) {
            tags.add(new KeyValuePair("componentName",
                context.getComponentName()));
        }

        return tags;
    }
}
