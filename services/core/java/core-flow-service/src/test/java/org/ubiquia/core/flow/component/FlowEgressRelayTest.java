package org.ubiquia.core.flow.component;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.repository.FlowMessageRepository;
import org.ubiquia.core.flow.repository.NodeRepository;
import org.ubiquia.core.flow.service.manager.NodeManager;
import org.ubiquia.core.flow.service.registrar.FlowMessageRegistrar;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FlowEgressRelayTest {

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private FlowMessageRegistrar flowMessageRegistrar;

    @Autowired
    private FlowMessageRepository flowMessageRepository;

    @Autowired
    private NodeManager nodeManager;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    public void assertTryPollAndForward_withNoPeers_skipsForwarding() {
        var relay = this.applicationContext.getBean(FlowEgressRelay.class);

        relay.tryPollAndForward();

        verify(this.restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    public void assertTryPollAndForward_withPeersAndNoMessages_skipsPost() {
        var relay = this.applicationContext.getBean(FlowEgressRelay.class);
        var peerUrls = (Set<String>) ReflectionTestUtils.getField(relay, "peerBaseUrls");
        peerUrls.add("http://peer-a:8080");

        relay.tryPollAndForward();

        verify(this.restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void assertTryPollAndForward_withOrphanedMessage_forwardsAndDeletes() throws Exception {
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

        Assertions.assertEquals(1L, this.flowMessageRepository.count());

        this.nodeManager.teardownAllNodes();

        var relay = this.applicationContext.getBean(FlowEgressRelay.class);
        var peerUrls = (Set<String>) ReflectionTestUtils.getField(relay, "peerBaseUrls");
        peerUrls.add("http://peer-a:8080");

        relay.tryPollAndForward();

        verify(this.restTemplate, atLeastOnce()).postForEntity(anyString(), any(), eq(Void.class));
        Assertions.assertEquals(0L, this.flowMessageRepository.count());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void assertTryPollAndForward_withFailingPost_doesNotDeleteMessage() throws Exception {
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

        this.nodeManager.teardownAllNodes();

        when(this.restTemplate.postForEntity(anyString(), any(), any()))
            .thenThrow(new RestClientException("connection refused"));

        var relay = this.applicationContext.getBean(FlowEgressRelay.class);
        var peerUrls = (Set<String>) ReflectionTestUtils.getField(relay, "peerBaseUrls");
        peerUrls.add("http://peer-a:8080");

        relay.tryPollAndForward();

        Assertions.assertEquals(1L, this.flowMessageRepository.count());
    }
}
