package org.ubiquia.core.flow.service.cluster;

import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.core.flow.component.FlowEgressRelay;
import org.ubiquia.core.flow.service.cluster.synchronization.AbstractSynchronizationService;
import org.ubiquia.core.flow.service.cluster.synchronization.AgentSynchronizationService;

/**
 * Top-level sync orchestrator. Combines peer URLs from {@link MicroweightSynchronizationService}
 * (JGroups/static) and {@link KubernetesSynchronizationService} (DB-discovered), then drives
 * all registered {@link AbstractSynchronizationService} implementations against the full peer set.
 *
 * <p>Enabled only when {@code ubiquia.cluster.flow-service.sync.enabled=true}.
 */
@ConditionalOnProperty(
    value = "ubiquia.cluster.flow-service.sync.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class ClusterSynchronizationService {

    private static final Logger logger =
        LoggerFactory.getLogger(ClusterSynchronizationService.class);

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private MicroweightSynchronizationService microweightSynchronizationService;

    @Autowired
    private KubernetesSynchronizationService kubernetesSynchronizationService;

    @Autowired
    private AgentSynchronizationService agentSynchronizationService;

    @Autowired
    private FlowEgressRelay flowEgressRelay;

    @Autowired
    private List<AbstractSynchronizationService<?, ?>> synchronizationServices;

    @Scheduled(
        fixedDelayString = "${ubiquia.cluster.flow-service.sync.frequency-milliseconds:30000}")
    public void tryBuildEgressRelays() {
        this.flowEgressRelay.updatePeers(new HashSet<>(this.resolvePeerUrls()));
    }

    @Scheduled(
        fixedDelayString = "${ubiquia.cluster.flow-service.sync.frequency-milliseconds:30000}")
    @Transactional
    public void synchronize() {
        var peerUrls = this.resolvePeerUrls();
        if (peerUrls.isEmpty()) {
            logger.debug("No peers resolved; skipping synchronization.");
            return;
        }

        logger.info("Starting synchronization with {} peer(s)...", peerUrls.size());

        var agentEntity = this.agentRepository.findById(this.agentConfig.getId()).orElse(null);
        if (Objects.isNull(agentEntity)) {
            logger.error("Cannot sync: agent record not found for id {}.", this.agentConfig.getId());
            return;
        }

        this.agentSynchronizationService.sync(peerUrls);

        for (var service : this.synchronizationServices) {
            service.sync(peerUrls, agentEntity);
        }

        logger.info("...synchronization complete.");
    }

    private List<String> resolvePeerUrls() {
        var urls = new LinkedHashSet<String>();
        urls.addAll(this.microweightSynchronizationService.resolvePeerUrls());
        urls.addAll(this.kubernetesSynchronizationService.resolvePeerUrls());
        return List.copyOf(urls);
    }
}
