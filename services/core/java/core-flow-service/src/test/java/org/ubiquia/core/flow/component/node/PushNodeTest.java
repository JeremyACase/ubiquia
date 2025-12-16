package org.ubiquia.core.flow.component.node;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.model.ubiquia.dto.GraphEdge;
import org.ubiquia.common.model.ubiquia.embeddable.EgressSettings;
import org.ubiquia.common.model.ubiquia.embeddable.NodeSettings;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;
import org.ubiquia.common.model.ubiquia.enums.HttpOutputType;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;


@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PushNodeTest {

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
    public void assertPOSTsListToEndpoint_isValid() throws Exception {

        var domainOntology = this.dummyFactory.generateDomainOntology();
        var graph = domainOntology.getGraphs().get(0);

        var hiddenComponent = this.dummyFactory.generateComponent();
        hiddenComponent.setComponentType(ComponentType.NONE);
        graph.getComponents().add(hiddenComponent);

        var ingressNode = this.dummyFactory.generateNode();
        ingressNode.setNodeType(NodeType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressNode.getInputSubSchemas().add(subSchema);
        ingressNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(ingressNode);

        var hiddenNode = this.dummyFactory.generateNode();
        hiddenNode.setNodeType(NodeType.HIDDEN);
        hiddenNode.setEgressSettings(new EgressSettings());
        hiddenNode.getEgressSettings().setHttpOutputType(HttpOutputType.POST);
        hiddenNode.setEndpoint("http://localhost:8080/test");
        hiddenNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        hiddenComponent.setNode(hiddenNode);
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
        var nodeContext = node.getNodeContext();

        var mockServer = MockRestServiceServer.createServer(this.restTemplate);
        mockServer
            .expect(ExpectedCount.once(), requestTo(nodeContext.getEndpointUri()))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess());

        var listOfStuff = new ArrayList<String>();
        listOfStuff.add("stuffOne");
        listOfStuff.add("stuffTwo");
        listOfStuff.add("stuffThree");

        node.push(this.objectMapper.writeValueAsString(listOfStuff));

        Thread.sleep(5000);
        mockServer.verify();
    }

    @Test
    public void assertPushesToEndpointAndMatchesEvent_isValid() throws Exception {

        var domainOntology = this.dummyFactory.generateDomainOntology();
        var graph = domainOntology.getGraphs().get(0);

        var hiddenComponent = this.dummyFactory.generateComponent();
        hiddenComponent.setComponentType(ComponentType.NONE);
        graph.getComponents().add(hiddenComponent);

        var ingresNode = this.dummyFactory.generateNode();
        ingresNode.setNodeType(NodeType.PUSH);
        ingresNode.setNodeSettings(new NodeSettings());
        ingresNode.getNodeSettings().setPersistInputPayload(true);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingresNode.getInputSubSchemas().add(subSchema);
        ingresNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(ingresNode);

        var hiddenNode = this.dummyFactory.generateNode();
        hiddenNode.setNodeType(NodeType.HIDDEN);
        hiddenNode.setEgressSettings(new EgressSettings());
        hiddenNode.getEgressSettings().setHttpOutputType(HttpOutputType.POST);
        hiddenNode.setEndpoint("http://localhost:8080/test");
        hiddenNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(hiddenNode);

        hiddenComponent.setNode(hiddenNode);

        var edge = new GraphEdge();
        edge.setLeftNodeName(ingresNode.getName());
        edge.setRightNodeNames(new ArrayList<>());
        edge.getRightNodeNames().add(hiddenNode.getName());
        graph.getEdges().add(edge);

        this.testHelper.registerAndDeploy(domainOntology, graph);

        var node = (PushNode) this
            .testHelper
            .findNode(ingresNode.getName(), graph.getName());

        var inputPayloadMap = new HashMap<String, String>();
        inputPayloadMap.put("testKey", UUID.randomUUID().toString());
        var json = this.objectMapper.writeValueAsString(inputPayloadMap);

        var response = node.push(json);

        var mappedPayload = this.objectMapper.convertValue(
            response.getBody().getInputPayload(),
            Map.class);

        Assertions.assertEquals(
            inputPayloadMap.get("testKey"),
            mappedPayload.get("testKey"));
    }
}