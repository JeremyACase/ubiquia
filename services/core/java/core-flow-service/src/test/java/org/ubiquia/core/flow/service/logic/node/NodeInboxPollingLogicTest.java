package org.ubiquia.core.flow.service.logic.node;

import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.ubiquia.common.model.ubiquia.dto.GraphEdge;
import org.ubiquia.common.model.ubiquia.embeddable.EgressSettings;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.component.node.EgressNode;
import org.ubiquia.core.flow.component.node.HiddenNode;
import org.ubiquia.core.flow.controller.DomainOntologyController;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.model.node.NodeContext;
import org.ubiquia.core.flow.repository.NodeRepository;
import org.ubiquia.core.flow.service.manager.NodeManager;


@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class NodeInboxPollingLogicTest {

    @Autowired
    private DomainOntologyController domainOntologyController;

    @Autowired
    private NodeInboxPollingLogic nodeInboxPollingLogic;

    @Autowired
    private NodeManager nodeManager;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private GraphController graphController;

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    public void assertTemplateNodeIsValidToPollInbox_isValid() {
        var node = new HiddenNode();
        var nodeContext = new NodeContext();
        node.setNodeContext(nodeContext);
        nodeContext.setNodeType(NodeType.HIDDEN);

        var valid = this.nodeInboxPollingLogic.isValidToPollInbox(node);
        Assertions.assertTrue(valid);
    }

    @Test
    public void assertNodeWithoutAgentIsValidToPollInbox_isValid() {
        var node = new EgressNode();
        var nodeContext = new NodeContext();
        node.setNodeContext(nodeContext);
        nodeContext.setEgressSettings(new EgressSettings());
        nodeContext.setNodeType(NodeType.EGRESS);

        var valid = this.nodeInboxPollingLogic.isValidToPollInbox(node);
        Assertions.assertTrue(valid);
    }

    @Test
    public void assertNodeWithoutAgentIsNotValidToPollInbox_isValid() {
        var node = new EgressNode();
        var nodeContext = new NodeContext();
        node.setNodeContext(nodeContext);
        nodeContext.setEgressSettings(new EgressSettings());
        nodeContext.setNodeType(NodeType.EGRESS);
        ReflectionTestUtils.setField(nodeContext, "openMessages", 100);

        var valid = this.nodeInboxPollingLogic.isValidToPollInbox(node);
        Assertions.assertFalse(valid);
    }

    @Test
    public void assertHiddenNodeValidToPollInbox_isValid() throws Exception {

        var domainOntology = this.dummyFactory.generateDomainOntology();
        var graph = domainOntology.getGraphs().get(0);

        var ingressNode = this.dummyFactory.generateNode();
        ingressNode.setNodeType(NodeType.PUSH);
        graph.getNodes().add(ingressNode);

        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressNode.getInputSubSchemas().add(subSchema);
        ingressNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));

        var hiddenNode = this.dummyFactory.generateNode();
        hiddenNode.setNodeType(NodeType.HIDDEN);
        hiddenNode.setEndpoint("http://localhost:8080/test");
        hiddenNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(hiddenNode);

        var edge = new GraphEdge();
        edge.setLeftNodeName(ingressNode.getName());
        edge.setRightNodeNames(new ArrayList<>());
        edge.getRightNodeNames().add(hiddenNode.getName());
        graph.getEdges().add(edge);

        this.testHelper.registerAndDeploy(domainOntology, graph);

        var node = this
            .testHelper
            .findNode(hiddenNode.getName(), graph.getName());

        var valid = this.nodeInboxPollingLogic.isValidToPollInbox(node);
        Assertions.assertTrue(valid);
    }

    @Test
    public void assertHiddenNodeIsNotValidToPollInbox_isValid() throws Exception {

        var domainOntology = this.dummyFactory.generateDomainOntology();
        var graph = domainOntology.getGraphs().get(0);

        var ingressNode = this.dummyFactory.generateNode();
        ingressNode.setNodeType(NodeType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressNode.getInputSubSchemas().add(subSchema);
        ingressNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(ingressNode);

        var hiddenNode = this.dummyFactory.generateNode();
        hiddenNode.setNodeType(NodeType.HIDDEN);
        hiddenNode.setEndpoint("http://localhost:8080/test");
        hiddenNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(hiddenNode);

        var hiddenComponent = this.dummyFactory.generateComponent();
        hiddenComponent.setNode(hiddenNode);
        graph.getComponents().add(hiddenComponent);

        var edge = new GraphEdge();
        edge.setLeftNodeName(ingressNode.getName());
        edge.setRightNodeNames(new ArrayList<>());
        edge.getRightNodeNames().add(hiddenNode.getName());
        graph.getEdges().add(edge);

        this.testHelper.registerAndDeploy(domainOntology, graph);

        var node = this
            .testHelper
            .findNode(hiddenNode.getName(), graph.getName());

        var nodeContext = node.getNodeContext();
        ReflectionTestUtils.setField(nodeContext, "openMessages", 100);

        var valid = this.nodeInboxPollingLogic.isValidToPollInbox(node);
        Assertions.assertFalse(valid);
    }
}
