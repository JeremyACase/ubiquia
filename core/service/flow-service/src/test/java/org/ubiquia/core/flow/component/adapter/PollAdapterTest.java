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
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.embeddable.PollSettings;
import org.ubiquia.common.model.ubiquia.enums.AdapterType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.mock.MockRegistrar;


@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PollAdapterTest {

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
    public void assertPollsEndpoint_isValid() throws Exception {
        var graph = this.dummyFactory.generateGraph();

        var pollAgent = this.dummyFactory.generateAgent();
        graph.getAgents().add(pollAgent);

        var pollAdapter = this.dummyFactory.generateAdapter();
        pollAdapter.setAdapterType(AdapterType.POLL);
        pollAdapter.setInputSubSchemas(new ArrayList<>());
        pollAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Person"));
        pollAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        pollAdapter.setPollSettings(new PollSettings());
        pollAdapter.getPollSettings().setPollFrequencyInMilliseconds(1000L);
        pollAdapter.getPollSettings().setPollEndpoint("http://localhost:8080/test");
        pollAgent.setAdapter(pollAdapter);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getGraphName());
        deployment.setVersion(graph.getVersion());
        this.graphController.tryDeployGraph(deployment);

        var mockServer = MockRestServiceServer.createServer(this.restTemplate);
        mockServer.expect(ExpectedCount.manyTimes(), requestTo(new URI(pollAdapter
                .getPollSettings()
                .getPollEndpoint())))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess());

        Thread.sleep(5000);
        mockServer.verify();
    }
}