package org.ubiquia.core.flow.component.adapter;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.mock.MockRegistrar;
import org.ubiquia.common.models.dto.GraphEdgeDto;
import org.ubiquia.common.models.embeddable.EgressSettings;
import org.ubiquia.common.models.embeddable.GraphDeployment;
import org.ubiquia.common.models.enums.AdapterType;
import org.ubiquia.common.models.enums.AgentType;
import org.ubiquia.common.models.enums.HttpOutputType;
import org.ubiquia.core.flow.service.command.adapter.MergeAdapterCommand;
import org.ubiquia.core.flow.service.mapper.FlowMessageDtoMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class MergeAdapterTest {

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private FlowMessageDtoMapper flowMessageDtoMapper;

    @Autowired
    private GraphController graphController;

    @Autowired
    private MergeAdapterCommand mergeAdapterCommand;

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
    public void assertPOSTsPayload_isValid() throws Exception {
        var graph = this.dummyFactory.generateGraph();

        var ingressAgent = this.dummyFactory.generateAgent();
        var hiddenAgentOne = this.dummyFactory.generateAgent();
        var mergeAgent = this.dummyFactory.generateAgent();
        mergeAgent.setAgentType(AgentType.NONE);
        graph.getAgents().add(ingressAgent);
        graph.getAgents().add(hiddenAgentOne);

        var hiddenAgentTwo = this.dummyFactory.generateAgent();
        graph.getAgents().add(hiddenAgentTwo);
        graph.getAgents().add(mergeAgent);

        var ingressAdapter = this.dummyFactory.generateAdapter();
        ingressAdapter.setAdapterType(AdapterType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressAdapter.getInputSubSchemas().add(subSchema);
        ingressAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));

        var hiddenAdapterOne = this.dummyFactory.generateAdapter();
        hiddenAdapterOne.setAdapterType(AdapterType.HIDDEN);
        hiddenAdapterOne.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        hiddenAdapterOne.setOutputSubSchema(this.dummyFactory.buildSubSchema("Person"));

        var hiddenAdapterTwo = this.dummyFactory.generateAdapter();
        hiddenAdapterTwo.setAdapterType(AdapterType.HIDDEN);
        hiddenAdapterTwo.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        hiddenAdapterTwo.setOutputSubSchema(this.dummyFactory.buildSubSchema("AdoptionTransaction"));

        var mergeAdapter = this.dummyFactory.generateAdapter();
        mergeAdapter.setAdapterType(AdapterType.MERGE);
        mergeAdapter.setEgressSettings(new EgressSettings());
        mergeAdapter.getEgressSettings().setHttpOutputType(HttpOutputType.POST);
        mergeAdapter.setEndpoint("http://localhost:8080/test");
        mergeAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Person"));
        mergeAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("AdoptionTransaction"));

        ingressAgent.setAdapter(ingressAdapter);
        hiddenAgentOne.setAdapter(hiddenAdapterOne);
        hiddenAgentTwo.setAdapter(hiddenAdapterTwo);
        mergeAgent.setAdapter(mergeAdapter);

        var edgeOne = new GraphEdgeDto();
        edgeOne.setLeftAdapterName(ingressAdapter.getAdapterName());
        edgeOne.setRightAdapterNames(new ArrayList<>());
        edgeOne.getRightAdapterNames().add(hiddenAdapterOne.getAdapterName());
        edgeOne.getRightAdapterNames().add(hiddenAdapterTwo.getAdapterName());
        graph.getEdges().add(edgeOne);

        var edgeTwo = new GraphEdgeDto();
        edgeTwo.setLeftAdapterName(hiddenAdapterOne.getAdapterName());
        edgeTwo.setRightAdapterNames(new ArrayList<>());
        edgeTwo.getRightAdapterNames().add(mergeAdapter.getAdapterName());
        graph.getEdges().add(edgeTwo);

        var edgeThree = new GraphEdgeDto();
        edgeThree.setLeftAdapterName(hiddenAdapterTwo.getAdapterName());
        edgeThree.setRightAdapterNames(new ArrayList<>());
        edgeThree.getRightAdapterNames().add(mergeAdapter.getAdapterName());
        graph.getEdges().add(edgeThree);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getGraphName());
        deployment.setVersion(graph.getVersion());
        this.graphController.tryDeployGraph(deployment);

        var pushAdapter = (PushAdapter) this
            .testHelper
            .findAdapter(ingressAdapter.getAdapterName(), graph.getGraphName());
        pushAdapter.push("test");

        var testAdapter = (MergeAdapter) this
            .testHelper
            .findAdapter(mergeAdapter.getAdapterName(), graph.getGraphName());

        var adapterContext = testAdapter.getAdapterContext();
        var mockServer = MockRestServiceServer.createServer(this.restTemplate);
        mockServer
            .expect(ExpectedCount.once(), requestTo(adapterContext.getEndpointUri()))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess());

        Thread.sleep(10000);
        mockServer.verify();
    }
}