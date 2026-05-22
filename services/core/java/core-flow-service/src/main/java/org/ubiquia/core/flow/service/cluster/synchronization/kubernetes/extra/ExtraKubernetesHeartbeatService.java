package org.ubiquia.core.flow.service.cluster.synchronization.kubernetes.extra;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Periodically probes statically-configured agents in external Kubernetes clusters and
 * maintains a reachability set used by {@link ExtraKubernetesSynchronizationService}.
 *
 * <p>Peer URLs are supplied via {@code ubiquia.cluster.kubernetes.extra.peer-base-urls}
 * (comma-separated). All configured peers are considered reachable at startup; repeated
 * probe failures beyond the configurable threshold mark a peer unreachable until it
 * recovers.
 *
 * <p>Enabled only when {@code ubiquia.kubernetes.enabled=true}.
 */
@ConditionalOnProperty(value = "ubiquia.kubernetes.enabled", havingValue = "true")
@Service
public class ExtraKubernetesHeartbeatService {

    private static final Logger logger =
        LoggerFactory.getLogger(ExtraKubernetesHeartbeatService.class);
    private static final String HEALTH_PATH = "/actuator/health";

    @Value("${ubiquia.cluster.kubernetes.extra.peer-base-urls:}")
    private String peerBaseUrlsConfig;

    @Value("${ubiquia.cluster.heartbeat.failure-threshold:3}")
    private int failureThreshold;

    private final RestTemplate healthTemplate;
    private final Set<String> reachablePeers = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, Integer> consecutiveFailures = new ConcurrentHashMap<>();

    public ExtraKubernetesHeartbeatService(final RestTemplateBuilder builder) {
        this.healthTemplate = builder
            .connectTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(3))
            .build();
    }

    @PostConstruct
    public void init() {
        if (this.peerBaseUrlsConfig.isBlank()) {
            return;
        }
        Arrays.stream(this.peerBaseUrlsConfig.split(","))
            .map(String::trim)
            .filter(url -> !url.isBlank())
            .forEach(this.reachablePeers::add);
        logger.info("Extra Kubernetes heartbeat initialized with {} configured peer(s).",
            this.reachablePeers.size());
    }

    @Scheduled(fixedDelayString = "${ubiquia.cluster.heartbeat.frequency-milliseconds:15000}")
    public void checkPeerHealth() {
        if (this.peerBaseUrlsConfig.isBlank()) {
            return;
        }
        Arrays.stream(this.peerBaseUrlsConfig.split(","))
            .map(String::trim)
            .filter(url -> !url.isBlank())
            .forEach(this::probe);
    }

    public Set<String> getReachablePeers() {
        return Set.copyOf(this.reachablePeers);
    }

    private void probe(final String peerUrl) {
        var url = peerUrl + HEALTH_PATH;
        try {
            var response = this.healthTemplate.getForEntity(url, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                onSuccess(peerUrl);
            } else {
                onFailure(peerUrl, "HTTP " + response.getStatusCode());
            }
        } catch (Exception e) {
            onFailure(peerUrl, e.getMessage());
        }
    }

    private void onSuccess(final String peerUrl) {
        this.consecutiveFailures.remove(peerUrl);
        if (this.reachablePeers.add(peerUrl)) {
            logger.info("Extra Kubernetes peer {} is reachable again; tombstone lifted.", peerUrl);
        }
    }

    private void onFailure(final String peerUrl, final String reason) {
        var failures = this.consecutiveFailures.merge(peerUrl, 1, Integer::sum);
        logger.warn("Extra Kubernetes peer {} probe failed ({}/{}): {}.",
            peerUrl, failures, this.failureThreshold, reason);
        if (failures >= this.failureThreshold && this.reachablePeers.remove(peerUrl)) {
            logger.warn("Extra Kubernetes peer {} tombstoned after {} consecutive probe failures.",
                peerUrl, failures);
        }
    }
}
