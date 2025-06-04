package org.ubiquia.core.flow.component.adapter;

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
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.model.ubiquia.dto.GraphEdgeDto;
import org.ubiquia.common.model.ubiquia.embeddable.BrokerSettings;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.enums.AdapterType;
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
public class PublishAdapterTest {

    @Autowired
    private GraphController graphController;

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private MockRegistrar mockRegistrar;

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
        var graph = this.dummyFactory.generateGraph();

        var ingressAgent = this.dummyFactory.generateAgent();
        graph.getAgents().add(ingressAgent);

        var ingressAdapter = this.dummyFactory.generateAdapter();
        ingressAdapter.setAdapterType(AdapterType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressAdapter.getInputSubSchemas().add(subSchema);
        ingressAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        ingressAgent.setAdapter(ingressAdapter);

        var publishAdapter = this.dummyFactory.generateAdapter();
        publishAdapter.setAdapterType(AdapterType.PUBLISH);
        publishAdapter.setBrokerSettings(new BrokerSettings());
        publishAdapter.getBrokerSettings().setTopic("topic.test");
        publishAdapter.getBrokerSettings().setType(BrokerType.KAFKA);
        publishAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        graph.getAgentlessAdapters().add(publishAdapter);

        var edge = new GraphEdgeDto();
        edge.setLeftAdapterName(ingressAdapter.getAdapterName());
        edge.setRightAdapterNames(new ArrayList<>());
        edge.getRightAdapterNames().add(publishAdapter.getAdapterName());
        graph.getEdges().add(edge);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getGraphName());
        deployment.setVersion(graph.getVersion());
        this.graphController.tryDeployGraph(deployment);

        var adapter = (PushAdapter) this
            .testHelper
            .findAdapter(ingressAdapter.getAdapterName(), graph.getGraphName());
        adapter.push("test");

        Thread.sleep(6000);
        Assertions.assertNotNull(this.mostRecentlyConsumedMessage);
    }
}