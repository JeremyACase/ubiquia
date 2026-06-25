package org.ubiquia.core.flow.service.registrar;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.ubiquia.common.model.ubiquia.dto.FlowEvent;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.repository.FlowMessageRepository;
import org.ubiquia.core.flow.repository.FlowRepository;
import org.ubiquia.core.flow.repository.NodeRepository;

/** Test class for FlowMessageRegistrarTest. */
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

    /** Sets up test fixtures. */
    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    public void assertTryRegisterFlowMessage_withValidNode_createsEntityChain() throws Exception {
        var domainOntology = this.dummyFactory.generateDomainOntology();
        final var graph = domainOntology.getGraphs().get(0);
        var node = this.dummyFactory.generateNode();
        node.setNodeType(NodeType.PUSH);
        node.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Person"));
        node.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(node);
        this.testHelper.registerAndDeploy(domainOntology, graph);

        var nodeEntity = this.nodeRepository
            .findByParentGraphNameAndName(graph.getName(), node.getName())
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

    @Test
    public void assertTryRegisterSync_withMissingFlowEvent_createsNoEntity() {
        var flowEventDto = new FlowEvent();
        flowEventDto.setId("aaaaaaaa-0000-0000-0000-000000000000");
        var nodeDto = new Node();
        nodeDto.setId("bbbbbbbb-0000-0000-0000-000000000000");
        var msg = new FlowMessage();
        msg.setFlowEvent(flowEventDto);
        msg.setTargetNode(nodeDto);
        msg.setPayload("{}");

        this.flowMessageRegistrar.tryRegisterSync(msg);

        Assertions.assertEquals(0L, this.flowMessageRepository.count());
    }

    @Test
    public void assertTryRegisterSync_withMissingNode_createsNoEntity() throws Exception {
        var ontology = this.dummyFactory.generateDomainOntology();
        final var graph = ontology.getGraphs().get(0);
        var node = this.dummyFactory.generateNode();
        node.setNodeType(NodeType.PUSH);
        node.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Person"));
        node.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(node);
        this.testHelper.registerAndDeploy(ontology, graph);

        var nodeEntity = this.nodeRepository
            .findByParentGraphNameAndName(graph.getName(), node.getName())
            .orElseThrow();
        var seedMsg = new FlowMessage();
        var seedNode = new Node();
        seedNode.setId(nodeEntity.getId());
        seedMsg.setTargetNode(seedNode);
        seedMsg.setPayload("{}");
        this.flowMessageRegistrar.tryRegisterFlowMessage(seedMsg);

        var flowEvent = this.flowEventRepository.findAll().iterator().next();
        var flowEventDto = new FlowEvent();
        flowEventDto.setId(flowEvent.getId());

        var msg = new FlowMessage();
        msg.setFlowEvent(flowEventDto);
        var missingNode = new Node();
        missingNode.setId("cccccccc-0000-0000-0000-000000000000");
        msg.setTargetNode(missingNode);
        msg.setPayload("{}");

        this.flowMessageRegistrar.tryRegisterSync(msg);

        Assertions.assertEquals(1L, this.flowMessageRepository.count());
    }

    @Test
    public void assertTryRegisterSync_withValidParents_createsEntity() throws Exception {
        var ontology = this.dummyFactory.generateDomainOntology();
        final var graph = ontology.getGraphs().get(0);
        var node = this.dummyFactory.generateNode();
        node.setNodeType(NodeType.PUSH);
        node.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Person"));
        node.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(node);
        this.testHelper.registerAndDeploy(ontology, graph);

        var nodeEntity = this.nodeRepository
            .findByParentGraphNameAndName(graph.getName(), node.getName())
            .orElseThrow();
        var seedMsg = new FlowMessage();
        var seedNode = new Node();
        seedNode.setId(nodeEntity.getId());
        seedMsg.setTargetNode(seedNode);
        seedMsg.setPayload("{}");
        this.flowMessageRegistrar.tryRegisterFlowMessage(seedMsg);

        var flowEvent = this.flowEventRepository.findAll().iterator().next();
        var flowEventDto = new FlowEvent();
        flowEventDto.setId(flowEvent.getId());

        var msg = new FlowMessage();
        msg.setFlowEvent(flowEventDto);
        var nodeDto = new Node();
        nodeDto.setId(nodeEntity.getId());
        msg.setTargetNode(nodeDto);
        msg.setPayload("{\"sync\":true}");

        this.flowMessageRegistrar.tryRegisterSync(msg);

        Assertions.assertEquals(2L, this.flowMessageRepository.count());
    }

    @Test
    public void assertTryRegisterSync_withDuplicateId_doesNotCreateDuplicate() throws Exception {
        var ontology = this.dummyFactory.generateDomainOntology();
        final var graph = ontology.getGraphs().get(0);
        var node = this.dummyFactory.generateNode();
        node.setNodeType(NodeType.PUSH);
        node.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Person"));
        node.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(node);
        this.testHelper.registerAndDeploy(ontology, graph);

        var nodeEntity = this.nodeRepository
            .findByParentGraphNameAndName(graph.getName(), node.getName())
            .orElseThrow();
        var seedMsg = new FlowMessage();
        var seedNode = new Node();
        seedNode.setId(nodeEntity.getId());
        seedMsg.setTargetNode(seedNode);
        seedMsg.setPayload("{}");
        this.flowMessageRegistrar.tryRegisterFlowMessage(seedMsg);

        var existing = this.flowMessageRepository.findAll().iterator().next();
        var flowEventDto = new FlowEvent();
        flowEventDto.setId(existing.getFlowEvent().getId());

        var msg = new FlowMessage();
        msg.setId(existing.getId());
        msg.setFlowEvent(flowEventDto);
        var nodeDto = new Node();
        nodeDto.setId(nodeEntity.getId());
        msg.setTargetNode(nodeDto);
        msg.setPayload("{}");

        this.flowMessageRegistrar.tryRegisterSync(msg);

        Assertions.assertEquals(1L, this.flowMessageRepository.count());
    }
}
