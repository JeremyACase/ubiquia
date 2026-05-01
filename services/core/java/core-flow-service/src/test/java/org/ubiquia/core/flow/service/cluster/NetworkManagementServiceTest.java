package org.ubiquia.core.flow.service.cluster;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;
import org.ubiquia.common.model.ubiquia.entity.NetworkEntity;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.repository.NetworkRepository;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class NetworkManagementServiceTest {

    @Autowired
    private NetworkManagementService networkManagementService;

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    @Transactional
    public void assertAgentInitialization_agentHasNetwork() {
        var agent = this.agentRepository
            .findById(this.agentConfig.getId())
            .orElseThrow(() -> new AssertionError("Agent should exist after initialization"));

        Assertions.assertNotNull(agent.getNetwork(),
            "Agent should have a network assigned after initialization");
    }

    @Test
    @Transactional
    public void assertOnSoloView_whenAgentHasNoNetwork_createsAndAssignsNetwork() {
        var agent = this.agentRepository.findById(this.agentConfig.getId()).orElseThrow();
        agent.setNetwork(null);
        this.agentRepository.save(agent);
        this.entityManager.flush();
        this.entityManager.clear();

        long networkCountBefore = this.networkRepository.count();

        this.networkManagementService.onSoloView();

        this.entityManager.flush();
        this.entityManager.clear();

        var updated = this.agentRepository.findById(this.agentConfig.getId()).orElseThrow();
        Assertions.assertNotNull(updated.getNetwork(),
            "Expected agent to have a network after onSoloView");
        Assertions.assertEquals(networkCountBefore + 1, this.networkRepository.count(),
            "Expected exactly one new network to be created");
    }

    @Test
    @Transactional
    public void assertOnSoloView_whenAgentAlreadyHasNetwork_isNoOp() {
        var agent = this.agentRepository.findById(this.agentConfig.getId()).orElseThrow();
        var existingNetworkId = agent.getNetwork().getId();

        long networkCountBefore = this.networkRepository.count();

        this.networkManagementService.onSoloView();

        this.entityManager.flush();
        this.entityManager.clear();

        var updated = this.agentRepository.findById(this.agentConfig.getId()).orElseThrow();
        Assertions.assertEquals(existingNetworkId, updated.getNetwork().getId(),
            "Expected network assignment to remain unchanged");
        Assertions.assertEquals(networkCountBefore, this.networkRepository.count(),
            "Expected no new networks to be created");
    }

    @Test
    @Transactional
    public void assertOnClusterView_withMultipleNetworks_consolidatesToSingleNetwork() {
        var secondNetwork = new NetworkEntity();
        secondNetwork = this.networkRepository.save(secondNetwork);

        var secondAgent = new AgentEntity();
        secondAgent.setId(UUID.randomUUID().toString());
        secondAgent.setDeployedGraphs(new ArrayList<>());
        secondAgent.setNetwork(secondNetwork);
        this.agentRepository.save(secondAgent);

        this.entityManager.flush();
        this.entityManager.clear();

        this.networkManagementService.onClusterView();

        this.entityManager.flush();
        this.entityManager.clear();

        var agents = new ArrayList<AgentEntity>();
        this.agentRepository.findAll().forEach(agents::add);

        Set<String> networkIds = agents.stream()
            .map(a -> a.getNetwork().getId())
            .collect(Collectors.toSet());

        Assertions.assertEquals(1, networkIds.size(),
            "Expected all agents to share one canonical network after cluster consolidation");
        Assertions.assertEquals(1, this.networkRepository.count(),
            "Expected orphaned network to be deleted");
    }

    @Test
    @Transactional
    public void assertOnClusterView_withNoNetworks_doesNotThrow() {
        var agents = new ArrayList<AgentEntity>();
        this.agentRepository.findAll().forEach(agents::add);
        for (var agent : agents) {
            agent.setNetwork(null);
            this.agentRepository.save(agent);
        }
        this.entityManager.flush();
        this.networkRepository.deleteAll();
        this.entityManager.flush();
        this.entityManager.clear();

        Assertions.assertDoesNotThrow(
            () -> this.networkManagementService.onClusterView(),
            "Expected onClusterView to handle no-network state without throwing");
    }
}
