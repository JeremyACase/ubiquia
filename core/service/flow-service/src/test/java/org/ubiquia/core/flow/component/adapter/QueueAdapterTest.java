package org.ubiquia.core.flow.component.adapter;

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
import org.ubiquia.common.model.ubiquia.adapter.QueueAdapterEgress;
import org.ubiquia.common.model.ubiquia.dto.GraphEdge;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.enums.AdapterType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.mock.MockRegistrar;


@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class QueueAdapterTest {

    @Autowired
    private GraphController graphController;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockRegistrar mockRegistrar;

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

        var graph = this.dummyFactory.generateGraph();

        var ingressComponent = this.dummyFactory.generateComponent();
        graph.getComponents().add(ingressComponent);

        var ingressAdapter = this.dummyFactory.generateAdapter();
        ingressAdapter.setAdapterType(AdapterType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressAdapter.getInputSubSchemas().add(subSchema);
        ingressAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        ingressComponent.setAdapter(ingressAdapter);

        var queueAdapter = this.dummyFactory.generateAdapter();
        queueAdapter.setAdapterType(AdapterType.QUEUE);
        queueAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        graph.getComponentlessAdapters().add(queueAdapter);

        var edge = new GraphEdge();
        edge.setLeftAdapterName(ingressAdapter.getName());
        edge.setRightAdapterNames(new ArrayList<>());
        edge.getRightAdapterNames().add(queueAdapter.getName());
        graph.getEdges().add(edge);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getName());
        deployment.setVersion(graph.getVersion());
        this.graphController.tryDeployGraph(deployment);

        var targetUrl = "http://localhost:8080/graph/"
            + graph.getName().toLowerCase()
            + "/adapter/"
            + queueAdapter.getName().toLowerCase()
            + "/queue/peek";

        var adapter = (PushAdapter) this
            .testHelper
            .findAdapter(ingressAdapter.getName(), graph.getName());

        var event = adapter.push("test").getBody();

        Thread.sleep(5000);

        var response = this.mockMvc.perform(MockMvcRequestBuilders
                .get(targetUrl)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .getResponse();

        var queueResponse = this.objectMapper.readValue(
            response.getContentAsString(),
            QueueAdapterEgress.class);

        Assertions.assertEquals(
            event.getBatchId(),
            queueResponse.getFlowEvent().getBatchId());
    }

    @Test
    public void assertPop_isValid() throws Exception {

        var graph = this.dummyFactory.generateGraph();

        var ingressAgent = this.dummyFactory.generateComponent();
        graph.getComponents().add(ingressAgent);

        var ingressAdapter = this.dummyFactory.generateAdapter();
        ingressAdapter.setAdapterType(AdapterType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressAdapter.getInputSubSchemas().add(subSchema);
        ingressAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        ingressAgent.setAdapter(ingressAdapter);

        var queueAdapter = this.dummyFactory.generateAdapter();
        queueAdapter.setAdapterType(AdapterType.QUEUE);
        queueAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        graph.getComponentlessAdapters().add(queueAdapter);

        var edge = new GraphEdge();
        edge.setLeftAdapterName(ingressAdapter.getName());
        edge.setRightAdapterNames(new ArrayList<>());
        edge.getRightAdapterNames().add(queueAdapter.getName());
        graph.getEdges().add(edge);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getName());
        deployment.setVersion(graph.getVersion());
        this.graphController.tryDeployGraph(deployment);

        var targetUrl = "http://localhost:8080/graph/"
            + graph.getName().toLowerCase()
            + "/adapter/"
            + queueAdapter.getName().toLowerCase()
            + "/queue/pop";

        var adapter = (PushAdapter) this
            .testHelper
            .findAdapter(ingressAdapter.getName(), graph.getName());

        var event = adapter.push("test").getBody();

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
            QueueAdapterEgress.class);

        var popEventTwo = this.objectMapper.readValue(
            responseTwo.getContentAsString(),
            QueueAdapterEgress.class);

        Assertions.assertEquals(
            event.getBatchId(),
            popEventOne.getFlowEvent().getBatchId());
        Assertions.assertEquals(
            0,
            popEventTwo.getQueuedRecords());
    }
}