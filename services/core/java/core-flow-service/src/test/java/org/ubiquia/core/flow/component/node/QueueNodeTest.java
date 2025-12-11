package org.ubiquia.core.flow.component.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.model.ubiquia.dto.GraphEdge;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.common.model.ubiquia.node.QueueNodeEgress;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.DomainOntologyController;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;


@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class QueueNodeTest {

    @Autowired
    private DomainOntologyController domainOntologyController;

    @Autowired
    private GraphController graphController;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    public void assertPeek_isValid() throws Exception {

        var domainOntology = this.dummyFactory.generateDomainOntology();
        var graph = domainOntology.getGraphs().get(0);

        var ingressComponent = this.dummyFactory.generateComponent();
        graph.getComponents().add(ingressComponent);

        var ingressNode = this.dummyFactory.generateNode();
        ingressNode.setNodeType(NodeType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressNode.getInputSubSchemas().add(subSchema);
        ingressNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        ingressComponent.setNode(ingressNode);

        var queueNode = this.dummyFactory.generateNode();
        queueNode.setNodeType(NodeType.QUEUE);
        queueNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(queueNode);

        var edge = new GraphEdge();
        edge.setLeftNodeName(ingressNode.getName());
        edge.setRightNodeNames(new ArrayList<>());
        edge.getRightNodeNames().add(queueNode.getName());
        graph.getEdges().add(edge);

        this.domainOntologyController.register(domainOntology);
        var deployment = new GraphDeployment();
        deployment.setGraphName(graph.getName());
        deployment.setDomainVersion(domainOntology.getVersion());
        deployment.setDomainOntologyName(domainOntology.getName());
        this.graphController.tryDeployGraph(deployment);

        var targetUrl = "http://localhost:8080/graph/"
            + graph.getName().toLowerCase()
            + "/node/"
            + queueNode.getName().toLowerCase()
            + "/queue/peek";

        var node = (PushNode) this
            .testHelper
            .findNode(ingressNode.getName(), graph.getName());

        var event = node.push("test").getBody();

        Thread.sleep(5000);

        var response = this.mockMvc.perform(MockMvcRequestBuilders
                .get(targetUrl)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .getResponse();

        var queueResponse = this.objectMapper.readValue(
            response.getContentAsString(),
            QueueNodeEgress.class);

        Assertions.assertEquals(
            event.getFlow().getId(),
            queueResponse.getFlowEvent().getFlow().getId());
    }

    @Test
    public void assertPop_isValid() throws Exception {

        var domainOntology = this.dummyFactory.generateDomainOntology();
        var graph = domainOntology.getGraphs().get(0);

        var ingressComponent = this.dummyFactory.generateComponent();
        graph.getComponents().add(ingressComponent);

        var ingressNode = this.dummyFactory.generateNode();
        ingressNode.setNodeType(NodeType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressNode.getInputSubSchemas().add(subSchema);
        ingressNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        ingressComponent.setNode(ingressNode);

        var queueNode = this.dummyFactory.generateNode();
        queueNode.setNodeType(NodeType.QUEUE);
        queueNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(queueNode);

        var edge = new GraphEdge();
        edge.setLeftNodeName(ingressNode.getName());
        edge.setRightNodeNames(new ArrayList<>());
        edge.getRightNodeNames().add(queueNode.getName());
        graph.getEdges().add(edge);

        this.domainOntologyController.register(domainOntology);
        var deployment = new GraphDeployment();
        deployment.setGraphName(graph.getName());
        deployment.setDomainVersion(domainOntology.getVersion());
        deployment.setDomainOntologyName(domainOntology.getName());
        this.graphController.tryDeployGraph(deployment);

        var targetUrl = "http://localhost:8080/graph/"
            + graph.getName().toLowerCase()
            + "/node/"
            + queueNode.getName().toLowerCase()
            + "/queue/pop";

        var node = (PushNode) this
            .testHelper
            .findNode(ingressNode.getName(), graph.getName());

        var event = node.push("test").getBody();

        Thread.sleep(5000);

        var responseOne = this.mockMvc.perform(MockMvcRequestBuilders
                .get(targetUrl)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .getResponse();

        var responseTwo = this.mockMvc.perform(MockMvcRequestBuilders
                .get(targetUrl)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .getResponse();

        var popEventOne = this.objectMapper.readValue(
            responseOne.getContentAsString(),
            QueueNodeEgress.class);

        var popEventTwo = this.objectMapper.readValue(
            responseTwo.getContentAsString(),
            QueueNodeEgress.class);

        Assertions.assertEquals(
            event.getFlow().getId(),
            popEventOne.getFlowEvent().getFlow().getId());
        Assertions.assertEquals(
            0,
            popEventTwo.getQueuedRecords());
    }
}