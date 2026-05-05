package org.ubiquia.core.flow.service.registrar;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.repository.FlowMessageRepository;
import org.ubiquia.core.flow.repository.FlowRepository;
import org.ubiquia.core.flow.repository.NodeRepository;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FlowMessageRegistrarTest {

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private FlowEventRepository flowEventRepository;

    @Autowired
    private FlowMessageRepository flowMessageRepository;

    @Autowired
    private FlowRepository flowRepository;

    @Autowired
    private FlowMessageRegistrar flowMessageRegistrar;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    public void assertTryRegisterFlowMessage_withValidNode_createsEntityChain() throws Exception {
        var domainOntology = this.dummyFactory.generateDomainOntology();
        var graph = domainOntology.getGraphs().get(0);
        var node = this.dummyFactory.generateNode();
        node.setNodeType(NodeType.PUSH);
        node.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Person"));
        node.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(node);
        this.testHelper.registerAndDeploy(domainOntology, graph);

        var nodeEntity = this.nodeRepository
            .findByGraphNameAndName(graph.getName(), node.getName())
            .orElseThrow();

        var nodeDto = new Node();
        nodeDto.setId(nodeEntity.getId());
        var flowMessage = new FlowMessage();
        flowMessage.setTargetNode(nodeDto);
        flowMessage.setPayload("{}");

        this.flowMessageRegistrar.tryRegisterFlowMessage(flowMessage);

        Assertions.assertEquals(1L, this.flowRepository.count());
        Assertions.assertEquals(1L, this.flowEventRepository.count());
        Assertions.assertEquals(1L, this.flowMessageRepository.count());
    }

    @Test
    public void assertTryRegisterFlowMessage_withInvalidNodeId_createsNoEntities() {
        var nodeDto = new Node();
        nodeDto.setId("nonexistent-node-id");
        var flowMessage = new FlowMessage();
        flowMessage.setTargetNode(nodeDto);
        flowMessage.setPayload("{}");

        this.flowMessageRegistrar.tryRegisterFlowMessage(flowMessage);

        Assertions.assertEquals(0L, this.flowRepository.count());
        Assertions.assertEquals(0L, this.flowEventRepository.count());
        Assertions.assertEquals(0L, this.flowMessageRepository.count());
    }
}
