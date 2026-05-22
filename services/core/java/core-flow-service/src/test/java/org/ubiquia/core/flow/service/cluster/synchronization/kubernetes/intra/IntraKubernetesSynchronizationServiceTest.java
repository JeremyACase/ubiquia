package org.ubiquia.core.flow.service.cluster.synchronization.kubernetes.intra;

import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.repository.NetworkRepository;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class IntraKubernetesSynchronizationServiceTest {

    @Autowired
    private IntraKubernetesSynchronizationService intraKubernetesSynchronizationService;

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    public void assertResolvePeerUrls_withNoPeers_returnsEmpty() {
        var urls = this.intraKubernetesSynchronizationService.resolvePeerUrls();
        Assertions.assertTrue(urls.isEmpty(), "Expected empty peer list when no peers exist");
    }

    @Test
    public void assertResolvePeerUrls_withReachablePeerAndBaseUrl_returnsPeerUrl() {
        var network = this.networkRepository.findAll().iterator().next();
        var peer = new AgentEntity();
        peer.setId(UUID.randomUUID().toString());
        peer.setDeployedGraphs(new ArrayList<>());
        peer.setBaseUrl("http://k8s-peer:8080");
        peer.setReachable(true);
        peer.setNetwork(network);
        this.agentRepository.save(peer);

        var urls = this.intraKubernetesSynchronizationService.resolvePeerUrls();

        Assertions.assertEquals(1, urls.size());
        Assertions.assertTrue(urls.contains("http://k8s-peer:8080"));
    }

    @Test
    public void assertResolvePeerUrls_withUnreachablePeer_excludesPeer() {
        var network = this.networkRepository.findAll().iterator().next();
        var peer = new AgentEntity();
        peer.setId(UUID.randomUUID().toString());
        peer.setDeployedGraphs(new ArrayList<>());
        peer.setBaseUrl("http://unreachable:8080");
        peer.setReachable(false);
        peer.setNetwork(network);
        this.agentRepository.save(peer);

        var urls = this.intraKubernetesSynchronizationService.resolvePeerUrls();

        Assertions.assertTrue(urls.isEmpty(), "Expected unreachable peer to be excluded");
    }

    @Test
    public void assertResolvePeerUrls_withPeerWithoutBaseUrl_excludesPeer() {
        var network = this.networkRepository.findAll().iterator().next();
        var peer = new AgentEntity();
        peer.setId(UUID.randomUUID().toString());
        peer.setDeployedGraphs(new ArrayList<>());
        peer.setBaseUrl(null);
        peer.setReachable(true);
        peer.setNetwork(network);
        this.agentRepository.save(peer);

        var urls = this.intraKubernetesSynchronizationService.resolvePeerUrls();

        Assertions.assertTrue(urls.isEmpty(), "Expected peer with no baseUrl to be excluded");
    }

    @Test
    public void assertResolvePeerUrls_excludesSelf() {
        var myAgent = this.agentRepository.findById(this.agentConfig.getId()).orElseThrow();
        myAgent.setBaseUrl("http://self:8080");
        this.agentRepository.save(myAgent);

        var urls = this.intraKubernetesSynchronizationService.resolvePeerUrls();

        Assertions.assertFalse(urls.contains("http://self:8080"),
            "Expected local agent to be excluded from its own peer list");
    }

    @Test
    public void assertResolvePeerUrls_withNoNetworkAssigned_returnsEmpty() {
        var myAgent = this.agentRepository.findById(this.agentConfig.getId()).orElseThrow();
        myAgent.setNetwork(null);
        this.agentRepository.save(myAgent);

        var urls = this.intraKubernetesSynchronizationService.resolvePeerUrls();

        Assertions.assertTrue(urls.isEmpty(), "Expected empty peer list when agent has no network");
    }
}
