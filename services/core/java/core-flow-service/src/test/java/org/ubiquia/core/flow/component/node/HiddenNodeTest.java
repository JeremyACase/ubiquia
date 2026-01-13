package org.ubiquia.core.flow.component.node;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
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
import org.ubiquia.common.model.ubiquia.enums.ComponentType;
import org.ubiquia.common.model.ubiquia.enums.EgressType;
import org.ubiquia.common.model.ubiquia.enums.HttpOutputType;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;


@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class HiddenNodeTest {

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
    public void assertPOSTsToEndpoint_isValid() throws Exception {

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

        node.push("test");

        Thread.sleep(10000);
        mockServer.verify();
    }

    @Test
    public void assertPOSTsToEndpointAsynchronously_isValid() throws Exception {

        try (var server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200));
            server.start();

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

            var egressSettings = new EgressSettings();
            egressSettings.setHttpOutputType(HttpOutputType.POST);
            egressSettings.setEgressType(EgressType.ASYNCHRONOUS);

            var hiddenNode = this.dummyFactory.generateNode();
            hiddenNode.setNodeType(NodeType.HIDDEN);
            hiddenNode.setEgressSettings(egressSettings);

            hiddenNode.setEndpoint(server.url("/test").toString());

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

            node.push("test");

            var req = server.takeRequest(30, TimeUnit.SECONDS);

            Assertions.assertEquals("POST", req.getMethod());
            Assertions.assertEquals("/test", req.getPath());
        }
    }

    @Test
    public void assertPUTsToEndpoint_isValid() throws Exception {

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
        hiddenNode.setEgressSettings(new EgressSettings());
        hiddenNode.getEgressSettings().setHttpOutputType(HttpOutputType.PUT);
        hiddenNode.setEndpoint("http://localhost:8080/test");
        hiddenNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(hiddenNode);

        var hiddenComponent = this.dummyFactory.generateComponent();
        hiddenComponent.setComponentType(ComponentType.NONE);
        graph.getComponents().add(hiddenComponent);
        hiddenComponent.setNode(hiddenNode);

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
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withSuccess());

        node.push("test");

        Thread.sleep(5000);
        mockServer.verify();
    }
}