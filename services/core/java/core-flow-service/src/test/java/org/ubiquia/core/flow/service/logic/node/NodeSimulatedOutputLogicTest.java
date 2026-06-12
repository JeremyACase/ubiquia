package org.ubiquia.core.flow.service.logic.node;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.ubiquia.common.model.ubiquia.dto.GraphEdge;
import org.ubiquia.common.model.ubiquia.embeddable.EgressSettings;
import org.ubiquia.common.model.ubiquia.enums.HttpOutputType;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.component.node.HiddenNode;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.service.builder.FlowEventBuilder;
import org.ubiquia.core.flow.service.proxy.TemplateComponentProxy;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class NodeSimulatedOutputLogicTest {

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private FlowEventBuilder flowEventBuilder;

    @Autowired
    private NodeSimulatedOutputLogic nodeSimulatedOutputLogic;

    @Autowired
    private TemplateComponentProxy templateComponentProxy;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    @Transactional
    public void assertIsSimulatedResponsePayload_whenTemplateComponentAutoLinkedByName_returnsTrue()
        throws Exception {

        var domainOntology = this.dummyFactory.generateDomainOntology();
        var graph = domainOntology.getGraphs().get(0);

        var ingressNode = this.dummyFactory.generateNode();
        ingressNode.setNodeType(NodeType.PUSH);
        ingressNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        ingressNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(ingressNode);

        var templateNode = this.dummyFactory.generateNode();
        templateNode.setNodeType(NodeType.HIDDEN);
        templateNode.setEgressSettings(new EgressSettings());
        templateNode.getEgressSettings().setHttpOutputType(HttpOutputType.PUT);
        templateNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        templateNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Cat"));
        graph.getNodes().add(templateNode);

        // Component shares the node's name — exercises the auto-link-by-name path in
        // GraphRegistrar. component.node and node.component are intentionally NOT set.
        var templateComponent = this.dummyFactory.generateComponent();
        templateComponent.setName(templateNode.getName());
        graph.getComponents().add(templateComponent);

        var edge = new GraphEdge();
        edge.setLeftNodeName(ingressNode.getName());
        edge.setRightNodeNames(new ArrayList<>());
        edge.getRightNodeNames().add(templateNode.getName());
        graph.getEdges().add(edge);

        this.testHelper.registerAndDeploy(domainOntology, graph);

        var deployedNode = (HiddenNode) this.testHelper.findNode(
            templateNode.getName(), graph.getName());

        Assertions.assertTrue(
            this.nodeSimulatedOutputLogic.isSimulatedResponsePayload(deployedNode));
    }

    @Test
    @Transactional
    public void assertIsSimulatedResponsePayload_whenNoComponent_returnsTrue() throws Exception {

        var domainOntology = this.dummyFactory.generateDomainOntology();
        var graph = domainOntology.getGraphs().get(0);

        var ingressNode = this.dummyFactory.generateNode();
        ingressNode.setNodeType(NodeType.PUSH);
        ingressNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        ingressNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(ingressNode);

        var plainNode = this.dummyFactory.generateNode();
        plainNode.setNodeType(NodeType.HIDDEN);
        plainNode.setEgressSettings(new EgressSettings());
        plainNode.getEgressSettings().setHttpOutputType(HttpOutputType.PUT);
        plainNode.setEndpoint("http://localhost:8080/test");
        plainNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        plainNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Cat"));
        graph.getNodes().add(plainNode);
        // No component added — node.getComponent() will be null in the context

        var edge = new GraphEdge();
        edge.setLeftNodeName(ingressNode.getName());
        edge.setRightNodeNames(new ArrayList<>());
        edge.getRightNodeNames().add(plainNode.getName());
        graph.getEdges().add(edge);

        this.testHelper.registerAndDeploy(domainOntology, graph);

        var deployedNode = (HiddenNode) this.testHelper.findNode(
            plainNode.getName(), graph.getName());

        Assertions.assertTrue(
            this.nodeSimulatedOutputLogic.isSimulatedResponsePayload(deployedNode));
    }

    @Test
    @Transactional
    public void assertOutputPayloadIsSet_whenTemplateComponent() throws Exception {

        var domainOntology = this.dummyFactory.generateDomainOntology();
        var graph = domainOntology.getGraphs().get(0);

        var ingressNode = this.dummyFactory.generateNode();
        ingressNode.setNodeType(NodeType.PUSH);
        ingressNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        ingressNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(ingressNode);

        var templateNode = this.dummyFactory.generateNode();
        templateNode.setNodeType(NodeType.HIDDEN);
        templateNode.setEgressSettings(new EgressSettings());
        templateNode.getEgressSettings().setHttpOutputType(HttpOutputType.PUT);
        templateNode.getNodeSettings().setPersistOutputPayload(true);
        templateNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        templateNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Cat"));
        graph.getNodes().add(templateNode);

        var templateComponent = this.dummyFactory.generateComponent();
        templateComponent.setName(templateNode.getName());
        graph.getComponents().add(templateComponent);

        var edge = new GraphEdge();
        edge.setLeftNodeName(ingressNode.getName());
        edge.setRightNodeNames(new ArrayList<>());
        edge.getRightNodeNames().add(templateNode.getName());
        graph.getEdges().add(edge);

        this.testHelper.registerAndDeploy(domainOntology, graph);

        var deployedNode = (HiddenNode) this.testHelper.findNode(
            templateNode.getName(), graph.getName());

        var flowEvent = this.flowEventBuilder.makeFlowAndEventFrom("test", deployedNode);
        this.templateComponentProxy.proxyAsComponentWith(flowEvent);

        Assertions.assertNotNull(flowEvent.getOutputPayload());
    }
}
