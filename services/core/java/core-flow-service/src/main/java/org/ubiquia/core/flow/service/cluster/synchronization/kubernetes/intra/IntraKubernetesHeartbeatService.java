package org.ubiquia.core.flow.service.cluster.synchronization.kubernetes.intra;

import jakarta.transaction.Transactional;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;

/**
 * Periodically probes every agent in this agent's network that has a {@code baseUrl}, and
 * tombstones (marks {@code reachable=false}) those that fail to respond after a configurable
 * number of consecutive attempts.
 *
 * <p>Enabled only on Kubernetes agents ({@code ubiquia.kubernetes.enabled=true}). Microweight
 * agents rely on JGroups failure detection instead.
 *
 * <p>When a previously unreachable agent responds again, it is automatically re-marked reachable
 * — re-registration from the remote side is not required.
 */
@ConditionalOnProperty(value = "ubiquia.kubernetes.enabled", havingValue = "true")
@Service
public class IntraKubernetesHeartbeatService {

    private static final Logger logger =
        LoggerFactory.getLogger(IntraKubernetesHeartbeatService.class);
    private static final String HEALTH_PATH = "/actuator/health";

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private IntraKubernetesReplicaClusterService replicaClusterService;

    @Value("${ubiquia.cluster.heartbeat.failure-threshold:3}")
    private int failureThreshold;

    private final RestTemplate healthTemplate;
    private final ConcurrentHashMap<String, Integer> consecutiveFailures =
        new ConcurrentHashMap<>();

    /** Builds the health-check RestTemplate with short connect and read timeouts. */
    public IntraKubernetesHeartbeatService(final RestTemplateBuilder builder) {
        this.healthTemplate = builder
            .connectTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(3))
            .build();
    }

    /** Probes all reachable intra-cluster peers and tombstones those that repeatedly fail. */
    @Scheduled(fixedDelayString = "${ubiquia.cluster.heartbeat.frequency-milliseconds:15000}")
    @Transactional
    public void checkPeerHealth() {
        if (!this.replicaClusterService.isLeader()) {
            logger.debug("Heartbeat skipped: not the replica cluster leader.");
            return;
        }
        var myAgent = this.agentRepository.findById(this.agentConfig.getId()).orElse(null);
        if (Objects.isNull(myAgent) || Objects.isNull(myAgent.getNetwork())) {
            logger.debug("Heartbeat skipped: local agent has no network assigned.");
            return;
        }

        var peers = this.agentRepository
            .findByNetworkAndBaseUrlIsNotNull(myAgent.getNetwork())
            .stream()
            .filter(a -> !a.getId().equals(this.agentConfig.getId()))
            .collect(Collectors.toList());

        if (peers.isEmpty()) {
            logger.debug("Heartbeat: no remote peers to probe in this network.");
            return;
        }

        logger.debug("Heartbeat: probing {} peer(s).", peers.size());
        for (var peer : peers) {
            probe(peer);
        }
    }

    private void probe(final AgentEntity peer) {
        var url = peer.getBaseUrl() + HEALTH_PATH;
        try {
            var response = this.healthTemplate.getForEntity(url, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                onSuccess(peer);
            } else {
                onFailure(peer, "HTTP " + response.getStatusCode());
            }
        } catch (Exception e) {
            onFailure(peer, e.getMessage());
        }
    }

    private void onSuccess(final AgentEntity peer) {
        this.consecutiveFailures.remove(peer.getId());
        if (!peer.isReachable()) {
            peer.setReachable(true);
            this.agentRepository.save(peer);
            logger.info("Agent {} is reachable again; tombstone lifted.", peer.getId());
        }
    }

    private void onFailure(final AgentEntity peer, final String reason) {
        var failures = this.consecutiveFailures.merge(peer.getId(), 1, Integer::sum);
        logger.warn("Agent {} probe failed ({}/{}): {}.",
            peer.getId(), failures, this.failureThreshold, reason);
        if (failures >= this.failureThreshold && peer.isReachable()) {
            peer.setReachable(false);
            this.agentRepository.save(peer);
            logger.warn("Agent {} tombstoned after {} consecutive probe failures.",
                peer.getId(), failures);
        }
    }
}
