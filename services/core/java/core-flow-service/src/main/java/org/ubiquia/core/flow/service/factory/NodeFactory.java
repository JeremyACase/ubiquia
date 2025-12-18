package org.ubiquia.core.flow.service.factory;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.entity.NodeEntity;
import org.ubiquia.common.model.ubiquia.entity.ComponentEntity;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.component.node.*;
import org.ubiquia.core.flow.service.builder.node.NodeBuilder;

/**
 * Because it wouldn't be Java without at least one factory.
 */
@Service
public class NodeFactory {

    private static final Logger logger = LoggerFactory.getLogger(NodeFactory.class);

    @Autowired
    private NodeBuilder nodeBuilder;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Make an adapter provided data from the database to do so with.
     *
     * @param nodeEntity The adapter data from the database.
     * @param graphEntity   The graph data from the database.
     * @return An adapter.
     * @throws Exception Exception from trying to build the adapter.
     */
    public AbstractNode makeNodeFor(
        final NodeEntity nodeEntity,
        final GraphEntity graphEntity,
        final GraphDeployment graphDeployment)
        throws Exception {

        logger.info("...building an node named {} for graph {}...",
            nodeEntity.getName(),
            graphEntity.getName());

        var node = this.makeNodeByType(nodeEntity.getNodeType());
        this.nodeBuilder.buildNodeFrom(node, nodeEntity, graphEntity, graphDeployment);

        logger.info("...completed building node {} for graph {}...",
            nodeEntity.getName(),
            nodeEntity.getGraph().getName());
        return node;
    }

    /**
     * Make an adapter for a component.
     *
     * @param componentEntity The component to make an adapter for.
     * @param graphDeployment The graph deployment requesting the adapter.
     * @return An adapter.
     * @throws Exception Exceptions from building the adapter.
     */
    public AbstractNode makeNodeFor(
        final ComponentEntity componentEntity,
        final GraphDeployment graphDeployment)
        throws Exception {

        logger.info("...building a node for graph {} and component {}...",
            componentEntity.getGraph().getName(),
            componentEntity.getName());

        if (Objects.isNull(componentEntity.getNode())) {
            throw new Exception("ERROR: Cannot build a node from an component with "
                + "a null node!");
        }

        var node = this.makeNodeByType(componentEntity.getNode().getNodeType());
        this.nodeBuilder.buildNodeFrom(
            node,
            componentEntity.getNode(),
            componentEntity.getGraph(),
            graphDeployment);

        logger.info("...completed building node for graph {} and component named {}...",
            componentEntity.getGraph().getName(),
            componentEntity.getName());

        return node;
    }

    /**
     * Make an adapter provided a type.
     *
     * @param nodeType The type of adapter to make.
     * @return A built adapter.
     * @throws Exception Exceptions from building the new adapter.
     */
    private AbstractNode makeNodeByType(final NodeType nodeType)
        throws Exception {
        AbstractNode node = null;

        switch (nodeType) {

            case EGRESS: {
                node = applicationContext.getBean(EgressNode.class);
            }
            break;

            case HIDDEN: {
                node = applicationContext.getBean(HiddenNode.class);
            }
            break;

            case MERGE: {
                node = applicationContext.getBean(MergeNode.class);
            }
            break;

            case POLL: {
                node = applicationContext.getBean(PollNode.class);
            }
            break;

            case PUBLISH: {
                node = applicationContext.getBean(PublishNode.class);
            }
            break;

            case PUSH: {
                node = applicationContext.getBean(PushNode.class);
            }
            break;

            case SUBSCRIBE: {
                node = applicationContext.getBean(SubscribeNode.class);
            }
            break;

            case QUEUE: {
                node = applicationContext.getBean(QueueNode.class);
            }
            break;

            default: {
                throw new Exception("ERROR: Unrecognized node type: "
                    + nodeType);
            }
        }
        return node;
    }
}
