package org.ubiquia.core.flow.component.node;

import java.util.ArrayList;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.model.ubiquia.dto.GraphEdge;
import org.ubiquia.common.model.ubiquia.embeddable.BrokerSettings;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.enums.BrokerType;
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
public class PublishNodeTest {

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

    private String mostRecentlyConsumedMessage;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @KafkaListener(topics = "topic.test")
    public void receive(ConsumerRecord<?, ?> consumerRecord) {
        this.mostRecentlyConsumedMessage = consumerRecord.value().toString();
    }

    @Test
    public void assertPublishesData_isValid() throws Exception {
        var domainOntology = this.dummyFactory.generateDomainOntology();
        var graph = domainOntology.getGraphs().get(0);

        var ingressComponent = this.dummyFactory.generateComponent();
        graph.getComponents().add(ingressComponent);

        var ingressNode = this.dummyFactory.generateNode();
        ingressNode.setNodeType(NodeType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressNode.getInputSubSchemas().add(subSchema);
        ingressNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        ingressComponent.setNode(ingressNode);

        var publishNode = this.dummyFactory.generateNode();
        publishNode.setNodeType(NodeType.PUBLISH);
        publishNode.setBrokerSettings(new BrokerSettings());
        publishNode.getBrokerSettings().setTopic("topic.test");
        publishNode.getBrokerSettings().setType(BrokerType.KAFKA);
        publishNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(publishNode);

        var edge = new GraphEdge();
        edge.setLeftNodeName(ingressNode.getName());
        edge.setRightNodeNames(new ArrayList<>());
        edge.getRightNodeNames().add(publishNode.getName());
        graph.getEdges().add(edge);

        this.domainOntologyController.register(domainOntology);
        var deployment = new GraphDeployment();
        deployment.setGraphName(graph.getName());
        deployment.setDomainVersion(domainOntology.getVersion());
        deployment.setDomainOntologyName(domainOntology.getName());
        this.graphController.tryDeployGraph(deployment);

        var node = (PushNode) this
            .testHelper
            .findNode(ingressNode.getName(), graph.getName());
        node.push("test");

        Thread.sleep(6000);
        Assertions.assertNotNull(this.mostRecentlyConsumedMessage);
    }
}