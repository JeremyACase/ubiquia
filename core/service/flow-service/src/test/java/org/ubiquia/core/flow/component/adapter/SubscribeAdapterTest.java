package org.ubiquia.core.flow.component.adapter;

import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.ubiquia.common.model.ubiquia.embeddable.BrokerSettings;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.enums.AdapterType;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;
import org.ubiquia.common.model.ubiquia.enums.BrokerType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.mock.MockRegistrar;

@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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
        this.testHelper.setupAgentState();
    }

    @Test
    public void assertSubscribesToData_isValid() throws Exception {
        var graph = this.dummyFactory.generateGraph();

        var subscribeComponent = this.dummyFactory.generateComponent();
        subscribeComponent.setComponentType(ComponentType.NONE);
        graph.getComponents().add(subscribeComponent);

        var subscribeAdapter = this.dummyFactory.generateAdapter();
        subscribeAdapter.setAdapterType(AdapterType.SUBSCRIBE);
        subscribeAdapter.setBrokerSettings(new BrokerSettings());
        subscribeAdapter.getBrokerSettings().setType(BrokerType.KAFKA);
        subscribeAdapter.getBrokerSettings().setTopic("topic.test");
        subscribeAdapter.setEndpoint("http://localhost:8080/test");
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        subscribeAdapter.getInputSubSchemas().add(subSchema);
        subscribeAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        subscribeComponent.setAdapter(subscribeAdapter);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getName());
        deployment.setVersion(graph.getVersion());
        this.graphController.tryDeployGraph(deployment);

        var adapter = (SubscribeAdapter) this
            .testHelper
            .findAdapter(subscribeAdapter.getName(), graph.getName());

        var mockWebServer = new MockWebServer();
        mockWebServer.url(adapter.getAdapterContext().getEndpointUri().toString());

        Thread.sleep(2000);
        this.kafkaTemplate.send("topic.test", "testMessage");
        Thread.sleep(2000);

        mockWebServer.shutdown();
    }
}