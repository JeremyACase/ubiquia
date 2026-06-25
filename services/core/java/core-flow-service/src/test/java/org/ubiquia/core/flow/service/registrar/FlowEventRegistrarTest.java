package org.ubiquia.core.flow.service.registrar;

import java.util.HashSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.ubiquia.common.model.ubiquia.dto.Flow;
import org.ubiquia.common.model.ubiquia.dto.FlowEvent;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.embeddable.FlowEventTimes;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.repository.FlowRepository;
import org.ubiquia.core.flow.repository.NodeRepository;

/** Test class for FlowEventRegistrarTest. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FlowEventRegistrarTest {

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private FlowEventRegistrar flowEventRegistrar;

    @Autowired
    private FlowEventRepository flowEventRepository;

    @Autowired
    private FlowMessageRegistrar flowMessageRegistrar;

    @Autowired
    private FlowRepository flowRepository;

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
    public void assertTryRegister_withNullFlowReference_throwsIllegalArgument() {
        var dto = new FlowEvent();
        dto.setNode(new Node());

        Assertions.assertThrows(IllegalArgumentException.class,
            () -> this.flowEventRegistrar.tryRegister(dto));
    }

    @Test
    public void assertTryRegister_withFlowNotFound_throwsIllegalArgument() {
        var flow = new Flow();
        flow.setId("aaaaaaaa-0000-0000-0000-000000000000");
        var node = new Node();
        node.setId("bbbbbbbb-0000-0000-0000-000000000000");
        var dto = new FlowEvent();
        dto.setFlow(flow);
        dto.setNode(node);

        Assertions.assertThrows(IllegalArgumentException.class,
            () -> this.flowEventRegistrar.tryRegister(dto));
    }

    @Test
    public void assertTryRegister_withNullNodeReference_throwsIllegalArgument() throws Exception {
        var chain = this.buildChain();

        var flow = new Flow();
        flow.setId(chain.flowId);
        var dto = new FlowEvent();
        dto.setFlow(flow);

        Assertions.assertThrows(IllegalArgumentException.class,
            () -> this.flowEventRegistrar.tryRegister(dto));
    }

    @Test
    public void assertTryRegister_withNodeNotFound_throwsIllegalArgument() throws Exception {
        var chain = this.buildChain();

        var flow = new Flow();
        flow.setId(chain.flowId);
        var node = new Node();
        node.setId("cccccccc-0000-0000-0000-000000000000");
        var dto = new FlowEvent();
        dto.setFlow(flow);
        dto.setNode(node);

        Assertions.assertThrows(IllegalArgumentException.class,
            () -> this.flowEventRegistrar.tryRegister(dto));
    }

    @Test
    public void assertTryRegister_withValidParents_createsFlowEventEntity() throws Exception {
        var chain = this.buildChain();
        final var countBefore = this.flowEventRepository.count();

        var flow = new Flow();
        flow.setId(chain.flowId);
        var node = new Node();
        node.setId(chain.nodeId);
        var dto = new FlowEvent();
        dto.setFlow(flow);
        dto.setNode(node);
        dto.setFlowEventTimes(new FlowEventTimes());

        this.flowEventRegistrar.tryRegister(dto);

        Assertions.assertEquals(countBefore + 1, this.flowEventRepository.count());
    }

    @Test
    public void assertTryRegister_withDuplicateId_doesNotCreateDuplicate() throws Exception {
        var chain = this.buildChain();

        var flow = new Flow();
        flow.setId(chain.flowId);
        var node = new Node();
        node.setId(chain.nodeId);
        var dto = new FlowEvent();
        dto.setFlow(flow);
        dto.setNode(node);
        dto.setFlowEventTimes(new FlowEventTimes());

        this.flowEventRegistrar.tryRegister(dto);
        final var countAfterFirst = this.flowEventRepository.count();

        // Re-register with the same ID that was auto-assigned
        var savedId = this.flowEventRepository.findAll().stream()
            .filter(e -> !e.getId().equals(chain.flowEventId))
            .findFirst()
            .orElseThrow()
            .getId();

        var dto2 = new FlowEvent();
        dto2.setId(savedId);
        dto2.setFlow(flow);
        dto2.setNode(node);
        dto2.setFlowEventTimes(new FlowEventTimes());
        this.flowEventRegistrar.tryRegister(dto2);

        Assertions.assertEquals(countAfterFirst, this.flowEventRepository.count());
    }

    private FlowEventChain buildChain() throws Exception {
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

        var nodeDto = new Node();
        nodeDto.setId(nodeEntity.getId());
        var msg = new FlowMessage();
        msg.setTargetNode(nodeDto);
        msg.setPayload("{}");
        this.flowMessageRegistrar.tryRegisterFlowMessage(msg);

        var flowEntity = this.flowRepository.findAll().iterator().next();
        var flowEventEntity = this.flowEventRepository.findAll().iterator().next();

        var chain = new FlowEventChain();
        chain.flowId = flowEntity.getId();
        chain.nodeId = nodeEntity.getId();
        chain.flowEventId = flowEventEntity.getId();
        return chain;
    }

    private static class FlowEventChain {
        String flowId;
        String nodeId;
        String flowEventId;
    }
}
