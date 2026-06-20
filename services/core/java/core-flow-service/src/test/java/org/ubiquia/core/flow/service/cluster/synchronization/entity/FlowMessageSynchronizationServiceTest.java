package org.ubiquia.core.flow.service.cluster.synchronization.entity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.repository.NodeRepository;
import org.ubiquia.core.flow.repository.SyncRepository;
import org.ubiquia.core.flow.service.registrar.FlowMessageRegistrar;

/** Test class for FlowMessageSynchronizationServiceTest. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FlowMessageSynchronizationServiceTest {

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private FlowMessageSynchronizationService flowMessageSynchronizationService;

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private FlowMessageRegistrar flowMessageRegistrar;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private SyncRepository syncRepository;

    @Autowired
    private TestHelper testHelper;

    /** Sets up test fixtures. */
    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    public void assertSync_withNoFlowMessages_doesNotPost() {
        var agentEntity = this.agentRepository.findById(this.agentConfig.getId()).orElseThrow();

        this.flowMessageSynchronizationService.sync(List.of("http://peer:8080"), agentEntity);

        verify(this.restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    public void assertSync_withFlowMessageAndPeerSuccess_postsAndCreatesSyncEntity()
        throws Exception {

        this.createFlowMessageChain();
        var agentEntity = this.agentRepository.findById(this.agentConfig.getId()).orElseThrow();

        this.flowMessageSynchronizationService.sync(List.of("http://peer:8080"), agentEntity);

        verify(this.restTemplate, atLeastOnce())
            .postForEntity(anyString(), any(), eq(Void.class));
        Assertions.assertTrue(this.syncRepository.count() > 0,
            "Expected SyncEntity records after successful sync");
    }

    @Test
    public void assertSync_withFlowMessageAndPeerFailure_doesNotCreateSyncEntity()
        throws Exception {

        doThrow(new RuntimeException("timeout"))
            .when(this.restTemplate).postForEntity(anyString(), any(), any());

        this.createFlowMessageChain();
        var agentEntity = this.agentRepository.findById(this.agentConfig.getId()).orElseThrow();

        this.flowMessageSynchronizationService.sync(List.of("http://peer:8080"), agentEntity);

        Assertions.assertEquals(0L, this.syncRepository.count(),
            "Expected no SyncEntity records when peer POST fails");
    }

    private void createFlowMessageChain() throws Exception {
        var ontology = this.dummyFactory.generateDomainOntology();
        final var graph = ontology.getGraphs().get(0);
        var node = this.dummyFactory.generateNode();
        node.setNodeType(NodeType.PUSH);
        node.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Person"));
        node.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(node);
        this.testHelper.registerAndDeploy(ontology, graph);

        var nodeEntity = this.nodeRepository
            .findByGraphNameAndName(graph.getName(), node.getName())
            .orElseThrow();

        var nodeDto = new Node();
        nodeDto.setId(nodeEntity.getId());
        var msg = new FlowMessage();
        msg.setTargetNode(nodeDto);
        msg.setPayload("{}");
        this.flowMessageRegistrar.tryRegisterFlowMessage(msg);
    }
}
