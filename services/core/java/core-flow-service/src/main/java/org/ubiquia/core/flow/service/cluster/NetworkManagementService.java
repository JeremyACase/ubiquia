package org.ubiquia.core.flow.service.cluster;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;
import org.ubiquia.common.model.ubiquia.entity.NetworkEntity;
import org.ubiquia.core.flow.repository.NetworkRepository;

@Service
public class NetworkManagementService {

    private static final Logger logger = LoggerFactory.getLogger(NetworkManagementService.class);

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * Called when this agent is the sole member of its cluster. Ensures the agent has a
     * solo NetworkEntity. If the agent already has one, this is a no-op.
     */
    @Transactional
    public void onSoloView() {
        logger.info("Solo view — ensuring agent has its own network...");

        var agentOpt = this.agentRepository.findById(this.agentConfig.getId());
        if (agentOpt.isEmpty()) {
            logger.warn("Agent record not found for id: {}", this.agentConfig.getId());
            return;
        }

        var agent = agentOpt.get();
        if (Objects.isNull(agent.getNetwork())) {
            var network = new NetworkEntity();
            network = this.networkRepository.save(network);
            agent.setNetwork(network);
            this.agentRepository.save(agent);
            logger.info("Created solo network {} for agent {}.", network.getId(), agent.getId());
        } else {
            logger.info("Agent {} already has network {}; no action needed.",
                agent.getId(), agent.getNetwork().getId());
        }
    }

    /**
     * Called when this agent has joined a multi-member cluster. Picks the oldest NetworkEntity
     * (by createdAt) as canonical, migrates all known agents to it, and deletes orphaned networks.
     */
    @Transactional
    public void onClusterView() {
        logger.info("Cluster view — consolidating agent networks...");

        var agents = new ArrayList<AgentEntity>();
        this.agentRepository.findAll().forEach(agents::add);

        var distinctNetworks = agents.stream()
            .map(AgentEntity::getNetwork)
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(
                NetworkEntity::getId,
                n -> n,
                (a, b) -> a))
            .values()
            .stream()
            .sorted(Comparator.comparing(NetworkEntity::getCreatedAt))
            .collect(Collectors.toList());

        if (distinctNetworks.isEmpty()) {
            logger.info("No networks found among known agents; nothing to consolidate.");
            return;
        }

        var canonical = distinctNetworks.get(0);
        logger.info("Canonical network: {}.", canonical.getId());

        for (var agent : agents) {
            if (Objects.isNull(agent.getNetwork())
                || !agent.getNetwork().getId().equals(canonical.getId())) {
                agent.setNetwork(canonical);
                this.agentRepository.save(agent);
                logger.info("Migrated agent {} to canonical network {}.",
                    agent.getId(), canonical.getId());
            }
        }

        // Flush agent FK updates before deleting orphaned networks to satisfy referential integrity.
        this.entityManager.flush();

        for (int i = 1; i < distinctNetworks.size(); i++) {
            var orphan = distinctNetworks.get(i);
            this.networkRepository.delete(orphan);
            logger.info("Deleted orphaned network {}.", orphan.getId());
        }

        logger.info("Network consolidation complete.");
    }
}
