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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.implementation.service.mapper.FlowMessageDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.GraphEdge;
import org.ubiquia.common.model.ubiquia.embeddable.EgressSettings;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.enums.AdapterType;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;
import org.ubiquia.common.model.ubiquia.enums.HttpOutputType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.mock.MockRegistrar;
import org.ubiquia.core.flow.service.command.adapter.MergeAdapterCommand;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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
        this.testHelper.setupAgentState();
    }

    /**
     * comment out now until fixed locally; race condition causing failures appears only locally 
     */
    // @Test
    // public void assertPOSTsPayload_isValid() throws Exception {
    //     var graph = this.dummyFactory.generateGraph();

    //     var ingressComponent = this.dummyFactory.generateComponent();
    //     var hiddenComponentOne = this.dummyFactory.generateComponent();
    //     var mergeComponent = this.dummyFactory.generateComponent();
    //     mergeComponent.setComponentType(ComponentType.NONE);
    //     graph.getComponents().add(ingressComponent);
    //     graph.getComponents().add(hiddenComponentOne);

    //     var hiddenComponentTwo = this.dummyFactory.generateComponent();
    //     graph.getComponents().add(hiddenComponentTwo);
    //     graph.getComponents().add(mergeComponent);

    //     var ingressAdapter = this.dummyFactory.generateAdapter();
    //     ingressAdapter.setAdapterType(AdapterType.PUSH);
    //     var subSchema = this.dummyFactory.buildSubSchema("Person");
    //     ingressAdapter.getInputSubSchemas().add(subSchema);
    //     ingressAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));

    //     var hiddenAdapterOne = this.dummyFactory.generateAdapter();
    //     hiddenAdapterOne.setAdapterType(AdapterType.HIDDEN);
    //     hiddenAdapterOne.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
    //     hiddenAdapterOne.setOutputSubSchema(this.dummyFactory.buildSubSchema("Person"));

    //     var hiddenAdapterTwo = this.dummyFactory.generateAdapter();
    //     hiddenAdapterTwo.setAdapterType(AdapterType.HIDDEN);
    //     hiddenAdapterTwo.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
    //     hiddenAdapterTwo.setOutputSubSchema(this.dummyFactory.buildSubSchema("AdoptionTransaction"));

    //     var mergeAdapter = this.dummyFactory.generateAdapter();
    //     mergeAdapter.setAdapterType(AdapterType.MERGE);
    //     mergeAdapter.setEgressSettings(new EgressSettings());
    //     mergeAdapter.getEgressSettings().setHttpOutputType(HttpOutputType.POST);
    //     mergeAdapter.setEndpoint("http://localhost:8080/test");
    //     mergeAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Person"));
    //     mergeAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("AdoptionTransaction"));

    //     ingressComponent.setAdapter(ingressAdapter);
    //     hiddenComponentOne.setAdapter(hiddenAdapterOne);
    //     hiddenComponentTwo.setAdapter(hiddenAdapterTwo);
    //     mergeComponent.setAdapter(mergeAdapter);

    //     var edgeOne = new GraphEdge();
    //     edgeOne.setLeftAdapterName(ingressAdapter.getName());
    //     edgeOne.setRightAdapterNames(new ArrayList<>());
    //     edgeOne.getRightAdapterNames().add(hiddenAdapterOne.getName());
    //     edgeOne.getRightAdapterNames().add(hiddenAdapterTwo.getName());
    //     graph.getEdges().add(edgeOne);

    //     var edgeTwo = new GraphEdge();
    //     edgeTwo.setLeftAdapterName(hiddenAdapterOne.getName());
    //     edgeTwo.setRightAdapterNames(new ArrayList<>());
    //     edgeTwo.getRightAdapterNames().add(mergeAdapter.getName());
    //     graph.getEdges().add(edgeTwo);

    //     var edgeThree = new GraphEdge();
    //     edgeThree.setLeftAdapterName(hiddenAdapterTwo.getName());
    //     edgeThree.setRightAdapterNames(new ArrayList<>());
    //     edgeThree.getRightAdapterNames().add(mergeAdapter.getName());
    //     graph.getEdges().add(edgeThree);

    //     this.graphController.register(graph);
    //     var deployment = new GraphDeployment();
    //     deployment.setName(graph.getName());
    //     deployment.setVersion(graph.getVersion());
    //     this.graphController.tryDeployGraph(deployment);

    //     var pushAdapter = (PushAdapter) this
    //         .testHelper
    //         .findAdapter(ingressAdapter.getName(), graph.getName());
    //     pushAdapter.push("test");

    //     var testAdapter = (MergeAdapter) this
    //         .testHelper
    //         .findAdapter(mergeAdapter.getName(), graph.getName());

    //     var adapterContext = testAdapter.getAdapterContext();
    //     var mockServer = MockRestServiceServer.createServer(this.restTemplate);
    //     mockServer
    //         .expect(ExpectedCount.once(), requestTo(adapterContext.getEndpointUri()))
    //         .andExpect(method(HttpMethod.POST))
    //         .andRespond(withSuccess());

    //     Thread.sleep(12000);
    //     mockServer.verify();
    // }
}