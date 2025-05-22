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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.mock.MockRegistrar;
import org.ubiquia.common.models.adapter.QueueAdapterEgress;
import org.ubiquia.common.models.dto.GraphEdgeDto;
import org.ubiquia.common.models.embeddable.GraphDeployment;
import org.ubiquia.common.models.enums.AdapterType;


@SpringBootTest
@AutoConfigureMockMvc
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
        this.testHelper.clearAllState();
    }

    @Test
    public void assertPeek_isValid() throws Exception {

        var graph = this.dummyFactory.generateGraph();

        var ingressAgent = this.dummyFactory.generateAgent();
        graph.getAgents().add(ingressAgent);

        var ingressAdapter = this.dummyFactory.generateAdapter();
        ingressAdapter.setAdapterType(AdapterType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressAdapter.getInputSubSchemas().add(subSchema);
        ingressAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        ingressAgent.setAdapter(ingressAdapter);

        var queueAdapter = this.dummyFactory.generateAdapter();
        queueAdapter.setAdapterType(AdapterType.QUEUE);
        queueAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        graph.getAgentlessAdapters().add(queueAdapter);

        var edge = new GraphEdgeDto();
        edge.setLeftAdapterName(ingressAdapter.getAdapterName());
        edge.setRightAdapterNames(new ArrayList<>());
        edge.getRightAdapterNames().add(queueAdapter.getAdapterName());
        graph.getEdges().add(edge);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getGraphName());
        deployment.setVersion(graph.getVersion());
        this.graphController.tryDeployGraph(deployment);

        var targetUrl = "http://localhost:8080/ubiquia/graph/"
            + graph.getGraphName().toLowerCase()
            + "/adapter/"
            + queueAdapter.getAdapterName().toLowerCase()
            + "/queue/peek";

        var adapter = (PushAdapter) this
            .testHelper
            .findAdapter(ingressAdapter.getAdapterName(), graph.getGraphName());

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

        var ingressAgent = this.dummyFactory.generateAgent();
        graph.getAgents().add(ingressAgent);

        var ingressAdapter = this.dummyFactory.generateAdapter();
        ingressAdapter.setAdapterType(AdapterType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressAdapter.getInputSubSchemas().add(subSchema);
        ingressAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        ingressAgent.setAdapter(ingressAdapter);

        var queueAdapter = this.dummyFactory.generateAdapter();
        queueAdapter.setAdapterType(AdapterType.QUEUE);
        queueAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        graph.getAgentlessAdapters().add(queueAdapter);

        var edge = new GraphEdgeDto();
        edge.setLeftAdapterName(ingressAdapter.getAdapterName());
        edge.setRightAdapterNames(new ArrayList<>());
        edge.getRightAdapterNames().add(queueAdapter.getAdapterName());
        graph.getEdges().add(edge);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getGraphName());
        deployment.setVersion(graph.getVersion());
        this.graphController.tryDeployGraph(deployment);

        var targetUrl = "http://localhost:8080/ubiquia/graph/"
            + graph.getGraphName().toLowerCase()
            + "/adapter/"
            + queueAdapter.getAdapterName().toLowerCase()
            + "/queue/pop";

        var adapter = (PushAdapter) this
            .testHelper
            .findAdapter(ingressAdapter.getAdapterName(), graph.getGraphName());

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