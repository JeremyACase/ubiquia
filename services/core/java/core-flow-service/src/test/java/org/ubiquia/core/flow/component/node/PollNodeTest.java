package org.ubiquia.core.flow.component.node;

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
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.DomainOntologyController;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;


@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PollNodeTest {

    @Autowired
    private GraphController graphController;

    @Autowired
    private DomainOntologyController domainOntologyController;

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
        var domainOntology = this.dummyFactory.generateDomainOntology();
        var graph = domainOntology.getGraphs().get(0);

        var pollComponent = this.dummyFactory.generateComponent();
        graph.getComponents().add(pollComponent);

        var pollNode = this.dummyFactory.generateNode();
        pollNode.setNodeType(NodeType.POLL);
        pollNode.setInputSubSchemas(new ArrayList<>());
        pollNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Person"));
        pollNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        pollNode.setPollSettings(new PollSettings());
        pollNode.getPollSettings().setPollFrequencyInMilliseconds(1000L);
        pollNode.getPollSettings().setPollEndpoint("http://localhost:8080/test");
        graph.getNodes().add(pollNode);
        pollComponent.setNode(pollNode);

        this.testHelper.registerAndDeploy(domainOntology, graph);

        var mockServer = MockRestServiceServer.createServer(this.restTemplate);
        mockServer.expect(ExpectedCount.manyTimes(), requestTo(new URI(pollNode
                .getPollSettings()
                .getPollEndpoint())))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess());

        Thread.sleep(5000);
        mockServer.verify();
    }
}