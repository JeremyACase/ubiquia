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
import org.ubiquia.common.library.implementation.service.builder.NodeEndpointRecordBuilder;
import org.ubiquia.common.library.implementation.service.mapper.NodeDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;
import org.ubiquia.common.model.ubiquia.entity.NodeEntity;
import org.ubiquia.core.flow.component.node.AbstractNode;
import org.ubiquia.core.flow.service.decorator.node.override.NodeOverrideDecorator;
import org.ubiquia.core.flow.service.logic.node.NodePassthroughLogic;
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
    private NodePassthroughLogic nodePassthroughLogic;
    @Autowired
    private NodeEndpointRecordBuilder nodeEndpointRecordBuilder;
    @Autowired
    private NodeTypeLogic nodeTypeLogic;

    /**
     * Build the provided adapter by using data from the database.
     *
     * @param node        The adapter to build.
     * @param nodeEntity  The adapter's data from the database.
     * @param graphEntity The graph's data from the database.
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

        logger.info("...building {} node named {} for graph {} with settings {}...",
            nodeEntity.getNodeType(),
            nodeEntity.getName(),
            graphEntity.getName(),
            graphDeployment.getGraphSettings());

        var nodeDatabaseData = this.nodeDtoMapper.map(nodeEntity);

        this
            .nodeOverrideDecorator
            .tryOverrideBaselineValues(
                nodeDatabaseData,
                nodeEntity.getOverrideSettings().stream().toList(),
                graphDeployment);

        var context = this
            .nodeContextBuilder
            .buildNodeContext(nodeDatabaseData, graphEntity, graphDeployment);
        node.setNodeContext(context);

        var tags = this.nodeTagBuilder.buildNodeTags(node);
        context.setTags(tags);

        this.trySetNodeEndpoint(node, nodeDatabaseData);
        node.initializeBehavior();

        logger.info("...built node...");
    }

    /**
     * Attempt to set a node's endpoint.
     *
     * @param node     The adapter to set.
     * @param nodeData The adapter data from the database.
     * @throws URISyntaxException Exceptions from parsing endpoints.
     */
    private void trySetNodeEndpoint(AbstractNode node, final Node nodeData)
        throws URISyntaxException {

        logger.info("...determining if node has an endpoint to build...");

        var componentData = nodeData.getComponent();
        var nodeContext = node.getNodeContext();

        var isPassthrough = this.nodePassthroughLogic.isPassthrough(node);
        if (!isPassthrough) {

            if (Objects.nonNull(componentData)) {
                var componentType = componentData.getComponentType();
                switch (componentType) {

                    // If we have a Kubernetes service to use...
                    case POD: {
                        logger.info("...building node endpoint for internal K8s service for "
                            + "{} component...",
                            componentType);
                        nodeContext
                            .setEndpointUri(this
                                .nodeEndpointRecordBuilder
                                .getComponentUriFrom(nodeData));
                        logger.info("...built endpoint: {}...", nodeContext.getEndpointUri());
                    }
                    break;

                    // ...use full endpoint for NONE type...
                    case NONE: {
                        logger.info("...assuming user-defined endpoint for {} component...",
                            componentType);
                        var uri = new URI(nodeData.getEndpoint());
                        nodeContext.setEndpointUri(uri);
                        logger.info("...built endpoint: {}...", nodeContext.getEndpointUri());
                    }
                    break;

                    // ...else just set to null
                    default: {
                        logger.info("...assuming no endpoint for {} component...",
                            componentType);
                        nodeContext.setEndpointUri(null);
                    }

                }
            } else if (this.nodeTypeLogic.nodeTypeRequiresEndpoint(nodeData.getNodeType())) {
                logger.info("...building user-provided endpoint for node...");
                var uri = new URI(nodeData.getEndpoint());
                nodeContext.setEndpointUri(uri);
                logger.info("...built endpoint {}...", nodeContext.getEndpointUri());
            }
        } else {
            logger.info("...node is assumed to be passthrough; not setting an endpoint...");
            nodeContext.setEndpointUri(null);
        }
        logger.info("...completed building node endpoint.");
    }
}
