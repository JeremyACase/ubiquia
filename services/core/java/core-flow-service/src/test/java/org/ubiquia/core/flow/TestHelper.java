package org.ubiquia.core.flow;

import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.core.flow.component.node.AbstractNode;
import org.ubiquia.core.flow.controller.DomainOntologyController;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.repository.NodeRepository;
import org.ubiquia.core.flow.service.logic.agent.AgentLogic;
import org.ubiquia.core.flow.service.manager.NodeManager;

@Service
public class TestHelper {

    @Autowired
    private DomainOntologyController domainOntologyController;

    @Autowired
    private GraphController graphController;

    @Autowired
    private NodeManager nodeManager;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private AgentLogic agentLogic;

    public void setupAgentState() {
        this.nodeManager.teardownAllNodes();
        this.agentLogic.tryInitializeAgentInDatabase();
    }

    public void registerAndDeploy(
        final DomainOntology domainOntology,
        final Graph graph)
        throws Exception {

        this.domainOntologyController.register(domainOntology);
        var deployment = new GraphDeployment();
        deployment.setGraphName(graph.getName());
        deployment.setDomainVersion(domainOntology.getVersion());
        deployment.setDomainOntologyName(domainOntology.getName());
        this.graphController.tryDeployGraph(deployment);
    }

    /**
     * A helper method that can "find" a deployed node from the context.
     *
     * @param nodeName  The node name to find.
     * @param graphName The graph to find the adapter from.
     * @return The deployed adapter.
     */
    @SuppressWarnings({"unchecked", "OptionalGetWithoutIsPresent"})
    public AbstractNode findNode(final String nodeName, final String graphName) {

        var nodeMap = (HashMap<String, HashMap<String, AbstractNode>>) ReflectionTestUtils
            .getField(this.nodeManager, "nodeMap");

        var nodeEntity = this
            .nodeRepository
            .findByGraphNameAndName(
                graphName,
                nodeName)
            .get();

        var node = nodeMap
            .get(graphName)
            .get(nodeEntity.getId());

        return node;
    }
}