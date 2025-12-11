package org.ubiquia.core.flow.service.builder.node;


import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import net.jimblackler.jsonschemafriend.GenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.implementation.service.builder.AdapterEndpointRecordBuilder;
import org.ubiquia.common.library.implementation.service.mapper.NodeDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.entity.NodeEntity;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;
import org.ubiquia.core.flow.component.node.AbstractNode;
import org.ubiquia.core.flow.service.decorator.node.override.NodeOverrideDecorator;
import org.ubiquia.core.flow.service.logic.node.NodeTypeLogic;

@Service
public class NodeBuilder {

    private static final Logger logger = LoggerFactory.getLogger(NodeBuilder.class);
    @Autowired
    private NodeDtoMapper nodeDtoMapper;
    @Autowired
    private NodeContextBuilder nodeContextBuilder;
    @Autowired
    private NodeOverrideDecorator nodeOverrideDecorator;
    @Autowired
    private NodeTagBuilder nodeTagBuilder;
    @Autowired
    private AdapterEndpointRecordBuilder adapterEndpointRecordBuilder;
    @Autowired
    private NodeTypeLogic nodeTypeLogic;

    /**
     * Build the provided adapter by using data from the database.
     *
     * @param node       The adapter to build.
     * @param nodeEntity The adapter's data from the database.
     * @param graphEntity   The graph's data from the database.
     * @throws URISyntaxException Exceptions from setting the URL that the adapter should
     *                            communicate with.
     */
    @Transactional
    public void buildNodeFrom(
        AbstractNode node,
        final NodeEntity nodeEntity,
        final GraphEntity graphEntity,
        final GraphDeployment graphDeployment)
        throws URISyntaxException,
        JsonProcessingException,
        IllegalAccessException,
        GenerationException {

        logger.info("...building {} adapter named {} for graph {} with settings {}...",
            nodeEntity.getNodeType(),
            nodeEntity.getName(),
            graphEntity.getName(),
            graphDeployment.getGraphSettings());

        var nodeDatabaseData = this.nodeDtoMapper.map(nodeEntity);

        this.nodeOverrideDecorator.tryOverrideBaselineValues(
            nodeDatabaseData,
            nodeEntity.getOverrideSettings().stream().toList(),
            graphDeployment);

        var context = this.nodeContextBuilder.buildNodeContext(
            nodeDatabaseData,
            graphEntity,
            graphDeployment);
        node.setNodeContext(context);

        var tags = this.nodeTagBuilder.buildAdapterTags(node);
        context.setTags(tags);

        this.trySetAdapterEndpoint(node, nodeDatabaseData);
        node.initializeBehavior();

        logger.info("...built adapter...");
    }


    /**
     * Attempt to set an adapter's endpoint.
     *
     * @param adapter     The adapter to set.
     * @param nodeData The adapter data from the database.
     * @throws URISyntaxException Exceptions from parsing endpoints.
     */
    private void trySetAdapterEndpoint(AbstractNode adapter, final Node nodeData)
        throws URISyntaxException {

        var componentData = nodeData.getComponent();
        var adapterContext = adapter.getNodeContext();
        if (!nodeData.getNodeSettings().getIsPassthrough()) {

            if (Objects.nonNull(componentData)) {

                switch (componentData.getComponentType()) {

                    // If we have a Kubernetes service to use...
                    case POD: {
                        adapterContext.setEndpointUri(
                            this.adapterEndpointRecordBuilder.getComponentUriFrom(nodeData));
                    }
                    break;

                    // ...use full endpoint for NONE type...
                    case NONE: {
                        adapterContext.setEndpointUri(new URI(nodeData.getEndpoint()));
                    }
                    break;

                    // ...else just set to null
                    default: {
                        adapterContext.setEndpointUri(null);
                    }

                }
            } else if (this.nodeTypeLogic.nodeTypeRequiresEndpoint(
                nodeData.getNodeType())) {

                adapterContext.setEndpointUri(new URI(nodeData.getEndpoint()));
            }
        }
    }
}
