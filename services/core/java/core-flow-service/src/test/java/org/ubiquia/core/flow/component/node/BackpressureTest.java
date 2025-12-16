package org.ubiquia.core.flow.component.node;

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
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class BackpressureTest {

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    @Transactional
    public void assertGetsBackPressure_isValid() throws Exception {

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
        hiddenNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(hiddenNode);

        var edge = new GraphEdge();
        edge.setLeftNodeName(ingressNode.getName());
        edge.setRightNodeNames(new ArrayList<>());
        edge.getRightNodeNames().add(hiddenNode.getName());
        graph.getEdges().add(edge);

        this.testHelper.registerAndDeploy(domainOntology, graph);

        var node = (HiddenNode) this
            .testHelper
            .findNode(hiddenNode.getName(), graph.getName());

        var backPressure = node.tryGetBackPressure();
        Assertions.assertTrue(backPressure.getStatusCode().is2xxSuccessful());
    }

    @Test
    @Transactional
    public void assertBackPressureValue_isValid() throws Exception {

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
        hiddenNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(hiddenNode);

        var edge = new GraphEdge();
        edge.setLeftNodeName(ingressNode.getName());
        edge.setRightNodeNames(new ArrayList<>());
        edge.getRightNodeNames().add(hiddenNode.getName());
        graph.getEdges().add(edge);

        this.testHelper.registerAndDeploy(domainOntology, graph);

        var node = (HiddenNode) this
            .testHelper
            .findNode(hiddenNode.getName(), graph.getName());

        var backPressure = node.tryGetBackPressure();
        Assertions.assertEquals(backPressure.getBody().getIngress().getQueuedRecords(), 0);
    }
}