package org.ubiquia.core.flow.service.manager;


import jakarta.transaction.Transactional;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.implementation.service.mapper.ComponentDtoMapper;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;
import org.ubiquia.common.model.ubiquia.entity.NodeEntity;
import org.ubiquia.core.flow.component.node.AbstractNode;
import org.ubiquia.core.flow.repository.GraphRepository;
import org.ubiquia.core.flow.service.command.manager.NodeManagerCommand;
import org.ubiquia.core.flow.service.factory.NodeFactory;
import org.ubiquia.core.flow.service.finder.GraphFinder;
import org.ubiquia.core.flow.service.visitor.ComponentCardinalityVisitor;

/**
 * This is a service that manages adapters at runtime. It is able to deploy them or tear them down
 * as necessary.
 */
@Service
public class NodeManager {

    private static final Logger logger = LoggerFactory.getLogger(NodeManager.class);
    private final HashMap<String, HashMap<String, AbstractNode>> nodeMap;
    @Autowired
    private NodeFactory nodeFactory;
    @Autowired
    private NodeManagerCommand nodeManagerCommand;
    @Autowired
    private ComponentDtoMapper componentDtoMapper;
    @Autowired
    private ComponentCardinalityVisitor componentCardinalityVisitor;
    @Autowired
    private GraphFinder graphFinder;
    @Autowired
    private GraphRepository graphRepository;

    public NodeManager() {
        this.nodeMap = new HashMap<>();
    }

    /**
     * Attempt to deploy adapters for a provided graph deployment.
     *
     * @param graphDeployment The graph deployment to deploy adapters for.
     * @throws Exception Exceptions from invalid data.
     */
    @Transactional
    public void tryDeployNodesFor(final GraphDeployment graphDeployment) throws Exception {

        logger.info("Deploying nodes for graph {}...", graphDeployment);

        var graphName = graphDeployment.getGraphName();
        var domainOntologyName = graphDeployment.getDomainOntologyName();
        var version = graphDeployment.getDomainVersion();

        var graphEntity = this
            .graphFinder
            .findGraphWith(graphName, domainOntologyName, version);


        for (var nodeEntity : graphEntity.getNodes()) {

            var nodeId = nodeEntity.getId();
            if (this.isNodeCurrentlyDeployedWith(graphName, nodeId)) {

                logger.warn("WARNING: node {} for graph {} is already deployed...",
                    nodeEntity.getName(),
                    graphEntity);

            } else {
                this.deployNodeFor(nodeEntity, graphEntity, graphDeployment);
            }
        }

        logger.info("...completed deploying nodes for graph named {}.",
            graphEntity.getName());
    }

    /**
     * Attempt to tear down all currently-deployed adapters.
     */
    @Transactional
    public void teardownAllNodes() {
        logger.info("Tearing down all deployed nodes...");
        for (var graphName : this.nodeMap.keySet()) {
            for (var adapterId : this.nodeMap.get(graphName).keySet()) {
                var adapter = this.nodeMap.get(graphName).get(adapterId);
                this.nodeManagerCommand.tearDown(adapter);
            }
        }
        this.nodeMap.clear();
        logger.info("...all adapters torn down.");
    }

    @Transactional
    public void tearDownNodesFor(final GraphDeployment deployment) {

        logger.info("Tearing down nodes for graph with name {}...",
            deployment.getGraphName());

        var version = deployment.getDomainVersion();

        var graphRecord = this
            .graphRepository
            .findByNameAndDomainOntologyNameAndDomainOntologyVersionMajorAndDomainOntologyVersionMinorAndDomainOntologyVersionPatch(
                deployment.getGraphName(),
                deployment.getDomainOntologyName(),
                version.getMajor(),
                version.getMinor(),
                version.getPatch());

        if (graphRecord.isPresent()) {
            if (this.nodeMap.containsKey(deployment.getGraphName())) {
                var graphEntity = graphRecord.get();
                for (var adapterEntity : graphEntity.getNodes()) {
                    var adapter = this.nodeMap
                        .get(graphEntity.getName())
                        .get(adapterEntity.getId());

                    this.nodeMap
                        .get(graphEntity.getName())
                        .remove(adapterEntity.getId());

                    this.nodeManagerCommand.tearDown(adapter);
                }
                logger.info("...completed tearing down nodes for graph with name {}...",
                    deployment.getGraphName());
            } else {
                logger.info("...no nodes present for graph: {}... ",
                    deployment.getGraphName());
            }
        } else {
            throw new IllegalArgumentException("ERROR: Could not tear down nodes; "
                + "graph doesn't exist with name "
                + deployment.getGraphName()
                + " and version "
                + deployment.getDomainVersion());
        }
    }

    private Boolean isNodeCurrentlyDeployedWith(final String graphName, final String nodeId) {

        var currentlyDeployed = false;
        if (this.nodeMap.containsKey(graphName)
            && this.nodeMap.get(graphName).containsKey(nodeId)) {

            currentlyDeployed = true;
        }
        return currentlyDeployed;
    }

    @Transactional
    private void deployNodeFor(
        final NodeEntity nodeEntity,
        final GraphEntity graphEntity,
        final GraphDeployment graphDeployment)
        throws Exception {

        logger.info("...deploying node named {} for graph {}...",
            nodeEntity.getName(),
            graphEntity.getName());

        var node = this
            .nodeFactory
            .makeNodeFor(nodeEntity, graphEntity, graphDeployment);

        var graphName = graphDeployment.getGraphName();

        if (!this.nodeMap.containsKey(graphName)) {
            this.nodeMap.put(graphName, new HashMap<>());
        }

        this.nodeMap
            .get(graphName)
            .put(node.getNodeContext().getNodeId(), node);

        logger.info("...deployed.");
    }
}
