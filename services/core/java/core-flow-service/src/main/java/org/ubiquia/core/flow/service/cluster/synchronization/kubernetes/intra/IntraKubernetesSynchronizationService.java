package org.ubiquia.core.flow.service.cluster.synchronization.kubernetes.intra;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.repository.AgentRepository;

/**
 * Resolves HTTP peer URLs for Kubernetes agents by querying the local database for agents in
 * this agent's network that have a {@code baseUrl} and are currently reachable.
 *
 * <p>These records are populated when microweight agents push their registration to a Kubernetes
 * peer via {@link org.ubiquia.core.flow.service.cluster.synchronization.entity.AgentSynchronizationService}.
 * The {@link IntraKubernetesHeartbeatService} is responsible for marking agents unreachable when
 * probes fail, which causes them to be excluded here until they recover.
 */
@Service
public class IntraKubernetesSynchronizationService {

    private static final Logger logger =
        LoggerFactory.getLogger(IntraKubernetesSynchronizationService.class);

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentRepository agentRepository;

    /**
     * Returns the base URLs of reachable Kubernetes peer agents known to this agent's network.
     */
    @Transactional
    public List<String> resolvePeerUrls() {
        var myAgent = this.agentRepository.findById(this.agentConfig.getId()).orElse(null);
        if (Objects.isNull(myAgent) || Objects.isNull(myAgent.getNetwork())) {
            logger.debug("No network assigned to local agent; no Kubernetes peers resolved.");
            return List.of();
        }

        var peers = this.agentRepository
            .findByNetworkAndBaseUrlIsNotNullAndReachableIsTrue(myAgent.getNetwork())
            .stream()
            .filter(a -> !a.getId().equals(this.agentConfig.getId()))
            .map(a -> a.getBaseUrl())
            .toList();

        if (!peers.isEmpty()) {
            logger.debug("Resolved {} Kubernetes peer(s) from network {}.",
                peers.size(), myAgent.getNetwork().getId());
        }

        return peers;
    }
}
