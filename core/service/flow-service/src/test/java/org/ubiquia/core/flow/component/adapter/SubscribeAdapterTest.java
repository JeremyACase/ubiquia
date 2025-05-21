package org.ubiquia.core.flow.component.adapter;

import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.mock.MockRegistrar;
import org.ubiquia.core.flow.model.embeddable.BrokerSettings;
import org.ubiquia.core.flow.model.embeddable.GraphDeployment;
import org.ubiquia.core.flow.model.enums.AdapterType;
import org.ubiquia.core.flow.model.enums.AgentType;
import org.ubiquia.core.flow.model.enums.BrokerType;

@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
public class SubscribeAdapterTest {

    @Autowired
    private GraphController graphController;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private MockRegistrar mockRegistrar;

    @Autowired
    private TestHelper testHelper;

    private String mostRecentlyConsumedMessage;

    @BeforeEach
    public void setup() {
        this.testHelper.clearAllState();
    }

    @Test
    public void assertSubscribesToData_isValid() throws Exception {
        var graph = this.dummyFactory.generateGraph();

        var subscribeAgent = this.dummyFactory.generateAgent();
        subscribeAgent.setAgentType(AgentType.NONE);
        graph.getAgents().add(subscribeAgent);

        var subscribeAdapter = this.dummyFactory.generateAdapter();
        subscribeAdapter.setAdapterType(AdapterType.SUBSCRIBE);
        subscribeAdapter.setBrokerSettings(new BrokerSettings());
        subscribeAdapter.getBrokerSettings().setType(BrokerType.KAFKA);
        subscribeAdapter.getBrokerSettings().setTopic("topic.test");
        subscribeAdapter.setEndpoint("http://localhost:8080/test");
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        subscribeAdapter.getInputSubSchemas().add(subSchema);
        subscribeAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        subscribeAgent.setAdapter(subscribeAdapter);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getGraphName());
        deployment.setVersion(graph.getVersion());
        this.graphController.tryDeployGraph(deployment);

        var adapter = (SubscribeAdapter) this
            .testHelper
            .findAdapter(subscribeAdapter.getAdapterName(), graph.getGraphName());

        var mockWebServer = new MockWebServer();
        mockWebServer.url(adapter.getAdapterContext().getEndpointUri().toString());

        Thread.sleep(2000);
        this.kafkaTemplate.send("topic.test", "testMessage");
        Thread.sleep(2000);

        mockWebServer.shutdown();
    }
}