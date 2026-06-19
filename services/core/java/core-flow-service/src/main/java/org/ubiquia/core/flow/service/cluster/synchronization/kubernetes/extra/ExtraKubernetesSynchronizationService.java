package org.ubiquia.core.flow.service.cluster.synchronization.kubernetes.extra;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Resolves HTTP peer URLs for agents running in external Kubernetes clusters.
 *
 * <p>Peers are supplied via {@code ubiquia.cluster.kubernetes.extra.peer-base-urls}
 * (comma-separated). Only peers currently deemed reachable by
 * {@link ExtraKubernetesHeartbeatService}
 * are returned. When the heartbeat service is not active (i.e., {@code ubiquia.kubernetes.enabled}
 * is not set), all configured URLs are returned unfiltered so that the relay can still attempt
 * delivery.
 */
@Service
public class ExtraKubernetesSynchronizationService {

    private static final Logger logger =
        LoggerFactory.getLogger(ExtraKubernetesSynchronizationService.class);

    @Autowired
    private Optional<ExtraKubernetesHeartbeatService> heartbeatService;

    @Value("${ubiquia.cluster.kubernetes.extra.peer-base-urls:}")
    private String peerBaseUrlsConfig;

    @Value("${ubiquia.cluster.sync.local-base-url:}")
    private String localBaseUrl;

    /**
     * Returns the base URLs of reachable agents in external Kubernetes clusters.
     */
    public List<String> resolvePeerUrls() {
        if (this.peerBaseUrlsConfig.isBlank()) {
            return List.of();
        }

        var configured = Arrays.stream(this.peerBaseUrlsConfig.split(","))
            .map(String::trim)
            .filter(url -> !url.isBlank() && !url.equals(this.localBaseUrl))
            .toList();

        var resolved = this.heartbeatService
            .map(svc -> configured.stream().filter(svc.getReachablePeers()::contains).toList())
            .orElse(configured);

        if (!resolved.isEmpty()) {
            logger.debug("Resolved {} extra Kubernetes peer(s) from config.", resolved.size());
        }

        return resolved;
    }
}
