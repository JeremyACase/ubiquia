package org.ubiquia.core.flow.service.visitor;

import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.model.ubiquia.dto.GraphEdge;
import org.ubiquia.common.model.ubiquia.embeddable.Cardinality;
import org.ubiquia.common.model.ubiquia.embeddable.CardinalitySetting;
import org.ubiquia.common.model.ubiquia.embeddable.EgressSettings;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;
import org.ubiquia.common.model.ubiquia.enums.HttpOutputType;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.DomainOntologyController;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;


@SpringBootTest
public class ComponentCardinalityVisitorTest {

    @Autowired
    private DomainOntologyController domainOntologyController;

    @Autowired
    private GraphController graphController;

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    public void assertDoesNotDeployAdapter_isValid() throws Exception {

        var domainOntology = this.dummyFactory.generateDomainOntology();
        var graph = domainOntology.getGraphs().get(0);

        var ingressComponent = this.dummyFactory.generateComponent();
        var hiddenComponent = this.dummyFactory.generateComponent();
        hiddenComponent.setComponentType(ComponentType.NONE);
        graph.getComponents().add(ingressComponent);
        graph.getComponents().add(hiddenComponent);

        var ingressNode = this.dummyFactory.generateNode();
        ingressNode.setNodeType(NodeType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressNode.getInputSubSchemas().add(subSchema);
        ingressNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));

        var hiddenNode = this.dummyFactory.generateNode();
        hiddenNode.setNodeType(NodeType.HIDDEN);
        hiddenNode.setEgressSettings(new EgressSettings());
        hiddenNode.getEgressSettings().setHttpOutputType(HttpOutputType.POST);
        hiddenNode.setEndpoint("http://localhost:8080/test");
        hiddenNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));

        ingressComponent.setNode(ingressNode);
        hiddenComponent.setNode(hiddenNode);

        var edge = new GraphEdge();
        edge.setLeftNodeName(ingressNode.getName());
        edge.setRightNodeNames(new ArrayList<>());
        edge.getRightNodeNames().add(hiddenNode.getName());
        graph.getEdges().add(edge);

        this.domainOntologyController.register(domainOntology);
        var deployment = new GraphDeployment();
        deployment.setGraphName(graph.getName());
        deployment.setDomainVersion(domainOntology.getVersion());
        deployment.setDomainOntologyName(domainOntology.getName());
        this.graphController.tryDeployGraph(deployment);

        var setting = new CardinalitySetting();
        setting.setReplicas(0);
        setting.setComponentName(ingressComponent.getName());

        var cardinality = new Cardinality();
        cardinality.setComponentSettings(new ArrayList<>());
        cardinality.getComponentSettings().add(setting);

        this.graphController.tryDeployGraph(deployment);

        // Should be null because our adapter's component's cardinality was toggled false
        var node = this.testHelper.findNode(ingressNode.getName(), graph.getName());
        Assertions.assertNull(node);
    }

    @Test
    public void assertDeploysAdapter_isValid() throws Exception {

        var domainOntology = this.dummyFactory.generateDomainOntology();
        var graph = domainOntology.getGraphs().get(0);

        var ingressComponent = this.dummyFactory.generateComponent();
        var hiddenComponent = this.dummyFactory.generateComponent();
        hiddenComponent.setComponentType(ComponentType.NONE);
        graph.getComponents().add(ingressComponent);
        graph.getComponents().add(hiddenComponent);

        var ingressNode = this.dummyFactory.generateNode();
        ingressNode.setNodeType(NodeType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressNode.getInputSubSchemas().add(subSchema);
        ingressNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));

        var hiddenNode = this.dummyFactory.generateNode();
        hiddenNode.setNodeType(NodeType.HIDDEN);
        hiddenNode.setEgressSettings(new EgressSettings());
        hiddenNode.getEgressSettings().setHttpOutputType(HttpOutputType.POST);
        hiddenNode.setEndpoint("http://localhost:8080/test");
        hiddenNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));

        ingressComponent.setNode(ingressNode);
        hiddenComponent.setNode(hiddenNode);

        var edge = new GraphEdge();
        edge.setLeftNodeName(ingressNode.getName());
        edge.setRightNodeNames(new ArrayList<>());
        edge.getRightNodeNames().add(hiddenNode.getName());
        graph.getEdges().add(edge);


        var setting = new CardinalitySetting();
        setting.setReplicas(1);
        setting.setComponentName(ingressComponent.getName());

        var cardinality = new Cardinality();
        cardinality.setComponentSettings(new ArrayList<>());
        cardinality.getComponentSettings().add(setting);

        this.domainOntologyController.register(domainOntology);
        var deployment = new GraphDeployment();
        deployment.setGraphName(graph.getName());
        deployment.setDomainVersion(domainOntology.getVersion());
        deployment.setDomainOntologyName(domainOntology.getName());
        this.graphController.tryDeployGraph(deployment);

        // Should not be null because our node's component's cardinality was set with a replica
        var node = this.testHelper.findNode(ingressNode.getName(), graph.getName());
        Assertions.assertNotNull(node);
    }
}