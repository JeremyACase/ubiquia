package org.ubiquia.core.flow.service.cluster.synchronization.kubernetes.extra;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
    "ubiquia.kubernetes.enabled=true",
    "ubiquia.cluster.kubernetes.extra.peer-base-urls=http://extra-peer-a:8080,http://extra-peer-b:8080",
    "ubiquia.cluster.heartbeat.failure-threshold=2"
})
public class ExtraKubernetesHeartbeatServiceTest {

    private static final String PEER_A = "http://extra-peer-a:8080";
    private static final String PEER_B = "http://extra-peer-b:8080";

    @MockBean
    private TaskScheduler taskScheduler;

    @Autowired
    private ExtraKubernetesHeartbeatService heartbeatService;

    private RestTemplate mockHealthTemplate;

    @BeforeEach
    public void setup() {
        this.mockHealthTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(this.heartbeatService, "healthTemplate", this.mockHealthTemplate);
        ReflectionTestUtils.setField(this.heartbeatService, "consecutiveFailures", new ConcurrentHashMap<>());
        var freshPeers = ConcurrentHashMap.newKeySet();
        ReflectionTestUtils.setField(this.heartbeatService, "reachablePeers", freshPeers);
        this.heartbeatService.init();
    }

    @Test
    public void assertInit_populatesReachablePeersFromConfig() {
        var reachable = this.heartbeatService.getReachablePeers();

        Assertions.assertTrue(reachable.contains(PEER_A), "Expected peer-a to be reachable after init");
        Assertions.assertTrue(reachable.contains(PEER_B), "Expected peer-b to be reachable after init");
    }

    @Test
    public void assertInit_withEmptyConfig_leavesReachablePeersEmpty() {
        ReflectionTestUtils.setField(this.heartbeatService, "peerBaseUrlsConfig", "");
        var freshPeers = ConcurrentHashMap.newKeySet();
        ReflectionTestUtils.setField(this.heartbeatService, "reachablePeers", freshPeers);

        this.heartbeatService.init();

        Assertions.assertTrue(this.heartbeatService.getReachablePeers().isEmpty(),
            "Expected empty reachable set when no peers configured");
    }

    @Test
    public void assertCheckPeerHealth_withEmptyConfig_doesNotProbe() {
        ReflectionTestUtils.setField(this.heartbeatService, "peerBaseUrlsConfig", "");

        this.heartbeatService.checkPeerHealth();

        verify(this.mockHealthTemplate, never()).getForEntity(anyString(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void assertCheckPeerHealth_probesAllConfiguredPeers() {
        when(this.mockHealthTemplate.getForEntity(anyString(), eq(Void.class)))
            .thenReturn(ResponseEntity.ok().build());

        this.heartbeatService.checkPeerHealth();

        verify(this.mockHealthTemplate, times(1))
            .getForEntity(eq(PEER_A + "/actuator/health"), eq(Void.class));
        verify(this.mockHealthTemplate, times(1))
            .getForEntity(eq(PEER_B + "/actuator/health"), eq(Void.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void assertCheckPeerHealth_afterFailureThreshold_tombstonesPeer() {
        when(this.mockHealthTemplate.getForEntity(eq(PEER_A + "/actuator/health"), eq(Void.class)))
            .thenThrow(new RuntimeException("connection refused"));
        when(this.mockHealthTemplate.getForEntity(eq(PEER_B + "/actuator/health"), eq(Void.class)))
            .thenReturn(ResponseEntity.ok().build());

        this.heartbeatService.checkPeerHealth();
        this.heartbeatService.checkPeerHealth();

        var reachable = this.heartbeatService.getReachablePeers();
        Assertions.assertFalse(reachable.contains(PEER_A),
            "Expected peer-a to be tombstoned after hitting failure threshold");
        Assertions.assertTrue(reachable.contains(PEER_B),
            "Expected peer-b to remain reachable");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void assertCheckPeerHealth_belowFailureThreshold_doesNotTombstone() {
        when(this.mockHealthTemplate.getForEntity(eq(PEER_A + "/actuator/health"), eq(Void.class)))
            .thenThrow(new RuntimeException("connection refused"));
        when(this.mockHealthTemplate.getForEntity(eq(PEER_B + "/actuator/health"), eq(Void.class)))
            .thenReturn(ResponseEntity.ok().build());

        this.heartbeatService.checkPeerHealth();

        Assertions.assertTrue(this.heartbeatService.getReachablePeers().contains(PEER_A),
            "Expected peer-a to still be reachable after only one failure (below threshold)");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void assertCheckPeerHealth_afterSuccessFollowingTombstone_restoresPeer() {
        when(this.mockHealthTemplate.getForEntity(eq(PEER_A + "/actuator/health"), eq(Void.class)))
            .thenThrow(new RuntimeException("connection refused"))
            .thenThrow(new RuntimeException("connection refused"))
            .thenReturn(ResponseEntity.ok().build());
        when(this.mockHealthTemplate.getForEntity(eq(PEER_B + "/actuator/health"), eq(Void.class)))
            .thenReturn(ResponseEntity.ok().build());

        this.heartbeatService.checkPeerHealth();
        this.heartbeatService.checkPeerHealth();
        Assertions.assertFalse(this.heartbeatService.getReachablePeers().contains(PEER_A),
            "Prerequisite: peer-a should be tombstoned before recovery test");

        this.heartbeatService.checkPeerHealth();

        Assertions.assertTrue(this.heartbeatService.getReachablePeers().contains(PEER_A),
            "Expected peer-a to be restored after a successful probe");
    }

    @Test
    public void assertGetReachablePeers_returnsImmutableSnapshot() {
        var snapshot = this.heartbeatService.getReachablePeers();

        Assertions.assertThrows(UnsupportedOperationException.class,
            () -> snapshot.add("http://injected:9999"),
            "Expected getReachablePeers to return an unmodifiable set");
    }
}
