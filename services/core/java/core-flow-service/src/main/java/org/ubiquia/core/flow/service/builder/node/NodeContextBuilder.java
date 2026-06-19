package org.ubiquia.core.flow.service.builder.node;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.implementation.service.builder.NodeEndpointRecordBuilder;
import org.ubiquia.common.library.implementation.service.mapper.ComponentDtoMapper;
import org.ubiquia.common.library.implementation.service.mapper.GraphDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;
import org.ubiquia.core.flow.model.node.NodeContext;
import org.ubiquia.core.flow.repository.ComponentRepository;
import org.ubiquia.core.flow.service.logic.node.NodeTypeLogic;

/** Builds {@link NodeContext} instances from node DTOs and graph entities at bootstrap time. */
@Service
public class NodeContextBuilder {

    private static final Logger logger = LoggerFactory.getLogger(NodeContextBuilder.class);

    @Autowired
    private ComponentDtoMapper componentDtoMapper;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private NodeEndpointRecordBuilder nodeEndpointRecordBuilder;

    @Autowired
    private GraphDtoMapper graphDtoMapper;

    @Autowired
    private NodeTypeLogic nodeTypeLogic;

    /** Builds a fully-initialized NodeContext from the given node, graph, and deployment. */
    public NodeContext buildNodeContext(
        final Node node,
        final GraphEntity graphEntity,
        final GraphDeployment graphDeployment)
        throws URISyntaxException,
        JsonProcessingException {

        var context = new NodeContext();

        context.setNodeName(node.getName());
        context.setNodeId(node.getId());
        context.setNodeType(node.getNodeType());
        context.setNodeSettings(node.getNodeSettings());
        context.setBackpressurePollRatePerMinute(
            this.getBackPressurePollRatePerMinute(node));
        context.setBrokerSettings(node.getBrokerSettings());
        context.setEgressSettings(node.getEgressSettings());
        context.setGraph(this.graphDtoMapper.map(graphEntity));
        context.setGraphSettings(graphDeployment.getGraphSettings());
        context.setPollSettings(node.getPollSettings());
        context.setComponent(node.getComponent());

        if (Objects.isNull(context.getComponent())) {
            var componentRecord = this.componentRepository
                .findByNameAndGraphId(node.getName(), graphEntity.getId());
            if (componentRecord.isPresent()) {
                logger.debug("Component not linked on node {}; resolved by name+graph lookup.",
                    node.getName());
                context.setComponent(this.componentDtoMapper.map(componentRecord.get()));
            }
        }

        this.trySetNodeContextEndpoint(context, node);

        return context;
    }

    private Long getBackPressurePollRatePerMinute(final Node nodeData) {

        var x = TimeUnit.MINUTES.toMillis(1);
        var y = nodeData.getNodeSettings().getBackpressurePollFrequencyMilliseconds();

        return x / y;
    }

    private void trySetNodeContextEndpoint(
        NodeContext nodeContext,
        final Node nodeData)
        throws URISyntaxException {

        if (Objects.nonNull(nodeData.getComponent())) {

            switch (nodeData.getComponent().getComponentType()) {

                // If we have a Kubernetes service to use...
                case POD: {
                    var uri = this.nodeEndpointRecordBuilder.getComponentUriFrom(nodeData);
                    nodeContext.setEndpointUri(uri);
                }
                break;

                // ...use full endpoint for NONE type...
                case NONE: {
                    var uri = new URI(nodeData.getEndpoint());
                    nodeContext.setEndpointUri(uri);
                }
                break;

                // ...else just set to null
                default:
                    {
                        nodeContext.setEndpointUri(null);
                    }

            }
        } else if (this.nodeTypeLogic.nodeTypeRequiresEndpoint(
            nodeData.getNodeType())) {

            var uri = new URI(nodeData.getEndpoint());
            nodeContext.setEndpointUri(uri);
        }
    }
}
