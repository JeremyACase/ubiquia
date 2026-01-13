package org.ubiquia.core.flow.component.node;

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
import org.ubiquia.common.model.ubiquia.enums.BrokerType;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.DomainOntologyController;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;

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
public class SubscribeNodeTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    public void assertSubscribesToData_isValid() throws Exception {
        var domainOntology = this.dummyFactory.generateDomainOntology();
        var graph = domainOntology.getGraphs().get(0);

        var subscribeComponent = this.dummyFactory.generateComponent();
        subscribeComponent.setComponentType(ComponentType.NONE);
        graph.getComponents().add(subscribeComponent);

        var subscribeNode = this.dummyFactory.generateNode();
        subscribeNode.setNodeType(NodeType.SUBSCRIBE);
        subscribeNode.setBrokerSettings(new BrokerSettings());
        subscribeNode.getBrokerSettings().setType(BrokerType.KAFKA);
        subscribeNode.getBrokerSettings().setTopic("topic.test");
        subscribeNode.setEndpoint("http://localhost:8080/test");
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        subscribeNode.getInputSubSchemas().add(subSchema);
        subscribeNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(subscribeNode);
        subscribeComponent.setNode(subscribeNode);

        this.testHelper.registerAndDeploy(domainOntology, graph);

        var node = (SubscribeNode) this
            .testHelper
            .findNode(subscribeNode.getName(), graph.getName());

        var mockWebServer = new MockWebServer();
        mockWebServer.url(node.getNodeContext().getEndpointUri().toString());

        Thread.sleep(2000);
        this.kafkaTemplate.send("topic.test", "testMessage");
        Thread.sleep(2000);

        mockWebServer.shutdown();
    }
}