package org.ubiquia.core.flow.component.adapter;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
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
import org.ubiquia.common.model.ubiquia.dto.GraphEdge;
import org.ubiquia.common.model.ubiquia.embeddable.EgressSettings;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.enums.AdapterType;
import org.ubiquia.common.model.ubiquia.enums.HttpOutputType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.mock.MockRegistrar;


@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EgressAdapterTest {

    @Autowired
    private GraphController graphController;

    @Autowired
    private MockRegistrar mockRegistrar;

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
    public void assertPOSTsToEndpoint_isValid() throws Exception {

        var graph = this.dummyFactory.generateGraph();

        var ingressComponent = this.dummyFactory.generateComponent();
        graph.getComponents().add(ingressComponent);

        var ingressAdapter = this.dummyFactory.generateAdapter();
        ingressAdapter.setAdapterType(AdapterType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressAdapter.getInputSubSchemas().add(subSchema);
        ingressAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        ingressComponent.setAdapter(ingressAdapter);

        var egressAdapter = this.dummyFactory.generateAdapter();
        egressAdapter.setAdapterType(AdapterType.EGRESS);
        egressAdapter.setEgressSettings(new EgressSettings());
        egressAdapter.getEgressSettings().setHttpOutputType(HttpOutputType.POST);
        egressAdapter.setEndpoint("http://localhost:8080/test");
        egressAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        graph.getComponentlessAdapters().add(egressAdapter);

        var edge = new GraphEdge();
        edge.setLeftAdapterName(ingressAdapter.getName());
        edge.setRightAdapterNames(new ArrayList<>());
        edge.getRightAdapterNames().add(egressAdapter.getName());
        graph.getEdges().add(edge);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getName());
        deployment.setVersion(graph.getVersion());
        this.graphController.tryDeployGraph(deployment);

        var mockServer = MockRestServiceServer.createServer(this.restTemplate);
        mockServer.expect(ExpectedCount.once(), requestTo(new URI(egressAdapter.getEndpoint())))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess());

        var adapter = (PushAdapter) this
            .testHelper
            .findAdapter(ingressAdapter.getName(), graph.getName());
        adapter.push("test");

        Thread.sleep(10000);
        mockServer.verify();
    }

    @Test
    public void assertPUTsToEndpoint_isValid() throws Exception {

        var graph = this.dummyFactory.generateGraph();

        var ingressComponent = this.dummyFactory.generateComponent();
        graph.getComponents().add(ingressComponent);

        var ingressAdapter = this.dummyFactory.generateAdapter();
        ingressAdapter.setAdapterType(AdapterType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressAdapter.getInputSubSchemas().add(subSchema);
        ingressAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        ingressComponent.setAdapter(ingressAdapter);

        var egressAdapter = this.dummyFactory.generateAdapter();
        egressAdapter.setAdapterType(AdapterType.EGRESS);
        egressAdapter.setEgressSettings(new EgressSettings());
        egressAdapter.getEgressSettings().setHttpOutputType(HttpOutputType.PUT);
        egressAdapter.setEndpoint("http://localhost:8080/test");
        egressAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        graph.getComponentlessAdapters().add(egressAdapter);

        var edge = new GraphEdge();
        edge.setLeftAdapterName(ingressAdapter.getName());
        edge.setRightAdapterNames(new ArrayList<>());
        edge.getRightAdapterNames().add(egressAdapter.getName());
        graph.getEdges().add(edge);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getName());
        deployment.setVersion(graph.getVersion());
        this.graphController.tryDeployGraph(deployment);

        var mockServer = MockRestServiceServer.createServer(this.restTemplate);
        mockServer.expect(ExpectedCount.once(), requestTo(new URI(egressAdapter.getEndpoint())))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withSuccess());

        var adapter = (PushAdapter) this
            .testHelper
            .findAdapter(ingressAdapter.getName(), graph.getName());
        adapter.push("test");

        Thread.sleep(10000);
        mockServer.verify();
    }
}