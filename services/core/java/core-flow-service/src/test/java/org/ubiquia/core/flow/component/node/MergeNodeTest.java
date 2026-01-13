package org.ubiquia.core.flow.component.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.ubiquia.common.model.ubiquia.dto.GraphEdge;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.repository.FlowMessageRepository;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MergeNodeTest {

    private static final Logger logger = LoggerFactory.getLogger(MergeNodeTest.class);

    @Autowired
    private FlowMessageRepository flowMessageRepository;

    @Autowired
    private FlowEventRepository flowEventRepository;

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    public void assertMergesMessages_isValid() throws Exception {
        var domainOntology = this.dummyFactory.generateDomainOntology();
        var graph = domainOntology.getGraphs().get(0);

        var ingressNode = this.dummyFactory.generateNode();
        ingressNode.setNodeType(NodeType.PUSH);
        ingressNode.setName("IngressNode");
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressNode.getInputSubSchemas().add(subSchema);
        ingressNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(ingressNode);

        var hiddenNodeA = this.dummyFactory.generateNode();
        hiddenNodeA.setNodeType(NodeType.HIDDEN);
        hiddenNodeA.setName("HiddenNodeA");
        subSchema = this.dummyFactory.buildSubSchema("Dog");
        hiddenNodeA.getInputSubSchemas().add(subSchema);
        hiddenNodeA.setOutputSubSchema(this.dummyFactory.buildSubSchema("Cat"));
        graph.getNodes().add(hiddenNodeA);

        var hiddenNodeB = this.dummyFactory.generateNode();
        hiddenNodeB.setNodeType(NodeType.HIDDEN);
        hiddenNodeB.setName("HiddenNodeB");
        subSchema = this.dummyFactory.buildSubSchema("Dog");
        hiddenNodeB.getInputSubSchemas().add(subSchema);
        hiddenNodeB.setOutputSubSchema(this.dummyFactory.buildSubSchema("Poodle"));
        graph.getNodes().add(hiddenNodeB);

        var mergeNode = this.dummyFactory.generateNode();
        mergeNode.setName("MergeNode");
        mergeNode.setNodeType(NodeType.MERGE);
        subSchema = this.dummyFactory.buildSubSchema("Cat");
        mergeNode.getInputSubSchemas().add(subSchema);
        subSchema = this.dummyFactory.buildSubSchema("Poodle");
        mergeNode.getInputSubSchemas().add(subSchema);
        mergeNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("AdoptionTransaction"));
        graph.getNodes().add(mergeNode);

        var edge = new GraphEdge();
        edge.setLeftNodeName(ingressNode.getName());
        edge.setRightNodeNames(new ArrayList<>());
        edge.getRightNodeNames().add(hiddenNodeA.getName());
        edge.getRightNodeNames().add(hiddenNodeB.getName());
        graph.getEdges().add(edge);

        edge = new GraphEdge();
        edge.setLeftNodeName(hiddenNodeA.getName());
        edge.setRightNodeNames(new ArrayList<>());
        edge.getRightNodeNames().add(mergeNode.getName());
        graph.getEdges().add(edge);

        edge = new GraphEdge();
        edge.setLeftNodeName(hiddenNodeB.getName());
        edge.setRightNodeNames(new ArrayList<>());
        edge.getRightNodeNames().add(mergeNode.getName());
        graph.getEdges().add(edge);

        this.testHelper.registerAndDeploy(domainOntology, graph);

        var pushNode = (PushNode) this
            .testHelper
            .findNode(ingressNode.getName(), graph.getName());

        var inputPayloadMap = new HashMap<String, String>();
        inputPayloadMap.put("testKeyA", UUID.randomUUID().toString());
        var json = this.objectMapper.writeValueAsString(inputPayloadMap);
        pushNode.push(json);

        Thread.sleep(5000);

        var flowMessages = this.flowMessageRepository.findAll();
        for (var flowMessage : flowMessages) {
            logger.info("Flow Id: {}", flowMessage.getFlowEvent().getFlow().getId());
        }

        var messagesCount = this.flowMessageRepository.count();
        var eventCount = this.flowEventRepository.count();

        Assertions.assertEquals(0, messagesCount);
        Assertions.assertEquals(4, eventCount);
    }
}