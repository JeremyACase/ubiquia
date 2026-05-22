package org.ubiquia.core.flow.service.cluster.synchronization.kubernetes.extra;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
    "ubiquia.kubernetes.enabled=true",
    "ubiquia.cluster.kubernetes.extra.peer-base-urls=http://extra-peer-a:8080,http://extra-peer-b:8080",
    "ubiquia.cluster.sync.local-base-url=http://localhost:8080",
    "ubiquia.cluster.heartbeat.failure-threshold=2"
})
public class ExtraKubernetesSynchronizationServiceTest {

    private static final String PEER_A = "http://extra-peer-a:8080";
    private static final String PEER_B = "http://extra-peer-b:8080";
    private static final String LOCAL = "http://localhost:8080";

    @Autowired
    private ExtraKubernetesSynchronizationService synchronizationService;

    @Autowired
    private ExtraKubernetesHeartbeatService heartbeatService;

    @BeforeEach
    public void setup() {
        var allPeers = ConcurrentHashMap.newKeySet();
        allPeers.add(PEER_A);
        allPeers.add(PEER_B);
        ReflectionTestUtils.setField(this.heartbeatService, "reachablePeers", allPeers);
    }

    @Test
    public void assertResolvePeerUrls_withNoConfig_returnsEmpty() {
        ReflectionTestUtils.setField(this.synchronizationService, "peerBaseUrlsConfig", "");

        var urls = this.synchronizationService.resolvePeerUrls();

        Assertions.assertTrue(urls.isEmpty(), "Expected empty list when no peers are configured");
    }

    @Test
    public void assertResolvePeerUrls_withAllReachablePeers_returnsAllConfigured() {
        var urls = this.synchronizationService.resolvePeerUrls();

        Assertions.assertEquals(2, urls.size());
        Assertions.assertTrue(urls.contains(PEER_A));
        Assertions.assertTrue(urls.contains(PEER_B));
    }

    @Test
    public void assertResolvePeerUrls_withTombstonedPeer_excludesTombstonedPeer() {
        var reachableOnly = ConcurrentHashMap.newKeySet();
        reachableOnly.add(PEER_B);
        ReflectionTestUtils.setField(this.heartbeatService, "reachablePeers", reachableOnly);

        var urls = this.synchronizationService.resolvePeerUrls();

        Assertions.assertEquals(1, urls.size());
        Assertions.assertTrue(urls.contains(PEER_B));
        Assertions.assertFalse(urls.contains(PEER_A),
            "Expected tombstoned peer-a to be excluded");
    }

    @Test
    public void assertResolvePeerUrls_withAllPeersTombstoned_returnsEmpty() {
        ReflectionTestUtils.setField(this.heartbeatService, "reachablePeers",
            ConcurrentHashMap.newKeySet());

        var urls = this.synchronizationService.resolvePeerUrls();

        Assertions.assertTrue(urls.isEmpty(),
            "Expected empty list when all peers are tombstoned");
    }

    @Test
    public void assertResolvePeerUrls_excludesLocalBaseUrl() {
        var allPeers = ConcurrentHashMap.newKeySet();
        allPeers.add(PEER_A);
        allPeers.add(PEER_B);
        allPeers.add(LOCAL);
        ReflectionTestUtils.setField(this.heartbeatService, "reachablePeers", allPeers);
        ReflectionTestUtils.setField(this.synchronizationService, "peerBaseUrlsConfig",
            PEER_A + "," + PEER_B + "," + LOCAL);

        var urls = this.synchronizationService.resolvePeerUrls();

        Assertions.assertFalse(urls.contains(LOCAL),
            "Expected local base URL to be excluded from peer list");
        Assertions.assertEquals(2, urls.size());
    }

    @Test
    public void assertResolvePeerUrls_withRestoredPeer_includesPeer() {
        var noPeers = ConcurrentHashMap.newKeySet();
        ReflectionTestUtils.setField(this.heartbeatService, "reachablePeers", noPeers);
        Assertions.assertTrue(this.synchronizationService.resolvePeerUrls().isEmpty(),
            "Prerequisite: no peers reachable");

        var allPeers = ConcurrentHashMap.newKeySet();
        allPeers.add(PEER_A);
        allPeers.add(PEER_B);
        ReflectionTestUtils.setField(this.heartbeatService, "reachablePeers", allPeers);

        var urls = this.synchronizationService.resolvePeerUrls();

        Assertions.assertEquals(2, urls.size(),
            "Expected both peers to appear after recovery");
    }
}
