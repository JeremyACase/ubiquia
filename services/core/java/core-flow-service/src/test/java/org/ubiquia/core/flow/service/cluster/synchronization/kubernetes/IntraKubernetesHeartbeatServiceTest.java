package org.ubiquia.core.flow.service.cluster.synchronization.kubernetes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.repository.NetworkRepository;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
    "ubiquia.kubernetes.enabled=true",
    "ubiquia.cluster.heartbeat.failure-threshold=2"
})
public class IntraKubernetesHeartbeatServiceTest {

    @Autowired
    private IntraKubernetesHeartbeatService heartbeatService;

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private TestHelper testHelper;

    private RestTemplate mockHealthTemplate;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
        this.mockHealthTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(this.heartbeatService, "healthTemplate", this.mockHealthTemplate);
        ReflectionTestUtils.setField(this.heartbeatService, "consecutiveFailures", new ConcurrentHashMap<>());
    }

    @Test
    public void assertCheckPeerHealth_withNoPeers_doesNotProbe() {
        this.heartbeatService.checkPeerHealth();

        verify(this.mockHealthTemplate, never()).getForEntity(anyString(), any());
    }

    @Test
    public void assertCheckPeerHealth_withPeerWithoutBaseUrl_doesNotProbe() {
        var network = this.networkRepository.findAll().iterator().next();
        var peer = new AgentEntity();
        peer.setId(UUID.randomUUID().toString());
        peer.setDeployedGraphs(new ArrayList<>());
        peer.setBaseUrl(null);
        peer.setReachable(true);
        peer.setNetwork(network);
        this.agentRepository.save(peer);

        this.heartbeatService.checkPeerHealth();

        verify(this.mockHealthTemplate, never()).getForEntity(anyString(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void assertCheckPeerHealth_afterFailureThreshold_tombstonesPeer() {
        var network = this.networkRepository.findAll().iterator().next();
        var peerId = UUID.randomUUID().toString();
        var peer = new AgentEntity();
        peer.setId(peerId);
        peer.setDeployedGraphs(new ArrayList<>());
        peer.setBaseUrl("http://peer:8080");
        peer.setReachable(true);
        peer.setNetwork(network);
        this.agentRepository.save(peer);

        when(this.mockHealthTemplate.getForEntity(anyString(), eq(Void.class)))
            .thenThrow(new RuntimeException("connection refused"));

        this.heartbeatService.checkPeerHealth();
        this.heartbeatService.checkPeerHealth();

        var updated = this.agentRepository.findById(peerId).orElseThrow();
        Assertions.assertFalse(updated.isReachable(),
            "Expected peer to be tombstoned after consecutive probe failures");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void assertCheckPeerHealth_afterSuccessFollowingFailures_liftsTombstone() {
        var network = this.networkRepository.findAll().iterator().next();
        var peerId = UUID.randomUUID().toString();
        var peer = new AgentEntity();
        peer.setId(peerId);
        peer.setDeployedGraphs(new ArrayList<>());
        peer.setBaseUrl("http://peer:8080");
        peer.setReachable(false);  // already tombstoned
        peer.setNetwork(network);
        this.agentRepository.save(peer);

        when(this.mockHealthTemplate.getForEntity(anyString(), eq(Void.class)))
            .thenReturn(ResponseEntity.ok().build());

        this.heartbeatService.checkPeerHealth();

        var updated = this.agentRepository.findById(peerId).orElseThrow();
        Assertions.assertTrue(updated.isReachable(),
            "Expected tombstone to be lifted after successful probe");
    }
}
