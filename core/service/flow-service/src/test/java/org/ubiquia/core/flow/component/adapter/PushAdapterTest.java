package org.ubiquia.core.flow.component.adapter;

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
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.model.ubiquia.dto.GraphEdgeDto;
import org.ubiquia.common.model.ubiquia.embeddable.AdapterSettings;
import org.ubiquia.common.model.ubiquia.embeddable.EgressSettings;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.enums.AdapterType;
import org.ubiquia.common.model.ubiquia.enums.AgentType;
import org.ubiquia.common.model.ubiquia.enums.HttpOutputType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.mock.MockRegistrar;


@SpringBootTest
@AutoConfigureMockMvc
public class PushAdapterTest {

    @Autowired
    private GraphController graphController;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private MockRegistrar mockRegistrar;

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
    public void assertPOSTsListToEndpoint_isValid() throws Exception {

        var graph = this.dummyFactory.generateGraph();

        var ingressAgent = this.dummyFactory.generateAgent();
        var hiddenAgent = this.dummyFactory.generateAgent();
        hiddenAgent.setAgentType(AgentType.NONE);
        graph.getAgents().add(ingressAgent);
        graph.getAgents().add(hiddenAgent);

        var ingressAdapter = this.dummyFactory.generateAdapter();
        ingressAdapter.setAdapterType(AdapterType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressAdapter.getInputSubSchemas().add(subSchema);
        ingressAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));

        var hiddenAdapter = this.dummyFactory.generateAdapter();
        hiddenAdapter.setAdapterType(AdapterType.HIDDEN);
        hiddenAdapter.setEgressSettings(new EgressSettings());
        hiddenAdapter.getEgressSettings().setHttpOutputType(HttpOutputType.POST);
        hiddenAdapter.setEndpoint("http://localhost:8080/test");
        hiddenAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));

        ingressAgent.setAdapter(ingressAdapter);
        hiddenAgent.setAdapter(hiddenAdapter);

        var edge = new GraphEdgeDto();
        edge.setLeftAdapterName(ingressAdapter.getAdapterName());
        edge.setRightAdapterNames(new ArrayList<>());
        edge.getRightAdapterNames().add(hiddenAdapter.getAdapterName());
        graph.getEdges().add(edge);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getGraphName());
        deployment.setVersion(graph.getVersion());
        this.graphController.tryDeployGraph(deployment);

        var adapter = (HiddenAdapter) this
            .testHelper
            .findAdapter(hiddenAdapter.getAdapterName(), graph.getGraphName());
        var adapterContext = adapter.getAdapterContext();

        var mockServer = MockRestServiceServer.createServer(this.restTemplate);
        mockServer
            .expect(ExpectedCount.once(), requestTo(adapterContext.getEndpointUri()))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess());

        var listOfStuff = new ArrayList<String>();
        listOfStuff.add("stuffOne");
        listOfStuff.add("stuffTwo");
        listOfStuff.add("stuffThree");

        adapter.push(this.objectMapper.writeValueAsString(listOfStuff));

        Thread.sleep(5000);
        mockServer.verify();
    }

    @Test
    public void assertPushesToEndpointAndMatchesEvent_isValid() throws Exception {

        var graph = this.dummyFactory.generateGraph();

        var ingressAgent = this.dummyFactory.generateAgent();
        var hiddenAgent = this.dummyFactory.generateAgent();
        hiddenAgent.setAgentType(AgentType.NONE);
        graph.getAgents().add(ingressAgent);
        graph.getAgents().add(hiddenAgent);

        var ingressAdapter = this.dummyFactory.generateAdapter();
        ingressAdapter.setAdapterType(AdapterType.PUSH);
        ingressAdapter.setAdapterSettings(new AdapterSettings());
        ingressAdapter.getAdapterSettings().setPersistInputPayload(true);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressAdapter.getInputSubSchemas().add(subSchema);
        ingressAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));

        var hiddenAdapter = this.dummyFactory.generateAdapter();
        hiddenAdapter.setAdapterType(AdapterType.HIDDEN);
        hiddenAdapter.setEgressSettings(new EgressSettings());
        hiddenAdapter.getEgressSettings().setHttpOutputType(HttpOutputType.POST);
        hiddenAdapter.setEndpoint("http://localhost:8080/test");
        hiddenAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));

        ingressAgent.setAdapter(ingressAdapter);
        hiddenAgent.setAdapter(hiddenAdapter);

        var edge = new GraphEdgeDto();
        edge.setLeftAdapterName(ingressAdapter.getAdapterName());
        edge.setRightAdapterNames(new ArrayList<>());
        edge.getRightAdapterNames().add(hiddenAdapter.getAdapterName());
        graph.getEdges().add(edge);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getGraphName());
        deployment.setVersion(graph.getVersion());
        this.graphController.tryDeployGraph(deployment);

        var adapter = (PushAdapter) this
            .testHelper
            .findAdapter(ingressAdapter.getAdapterName(), graph.getGraphName());

        var inputPayloadMap = new HashMap<String, String>();
        inputPayloadMap.put("testKey", UUID.randomUUID().toString());
        var json = this.objectMapper.writeValueAsString(inputPayloadMap);

        var response = adapter.push(json);

        var mappedPayload = this.objectMapper.convertValue(
            response.getBody().getInputPayload(),
            Map.class);

        Assertions.assertEquals(inputPayloadMap.get("testKey"),
            mappedPayload.get("testKey"));
    }
}