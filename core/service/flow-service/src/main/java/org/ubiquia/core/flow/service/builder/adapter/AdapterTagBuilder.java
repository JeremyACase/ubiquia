package org.ubiquia.core.flow.service.builder.adapter;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.embeddable.KeyValuePair;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;

@Service
public class AdapterTagBuilder {

    /**
     * Build tags for a provided adapter.
     *
     * @param adapter The adapter to build tags for.
     * @return The list of tags.
     */
    public List<KeyValuePair> buildAdapterTags(final AbstractAdapter adapter) {

        var tags = new ArrayList<KeyValuePair>();

        var context = adapter.getAdapterContext();
        tags.add(new KeyValuePair("adapterType", context.getAdapterType().toString()));
        tags.add(new KeyValuePair("graphName", context.getGraphName()));
        tags.add(new KeyValuePair("adapterName", context.getAdapterName()));

        if (Objects.nonNull(context.getComponentName())) {
            tags.add(new KeyValuePair("componentName",
                context.getComponentName()));
        }

        return tags;
    }
}
