package org.ubiquia.core.flow.controller;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.common.model.ubiquia.dto.Agent;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;
import org.ubiquia.core.flow.TestHelper;

/** Test class for AgentControllerTest. */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AgentControllerTest {

    @Autowired
    private AgentController agentController;

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private TestHelper testHelper;

    /** Sets up test fixtures. */
    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    @Transactional
    public void assertRegister_newAgent_persistsWithBaseUrlAndNetwork() {
        var remoteId = UUID.randomUUID().toString();
        var dto = new Agent();
        dto.setId(remoteId);
        dto.setBaseUrl("http://remote:8080");

        this.agentController.register(dto);

        var saved = this.agentRepository.findById(remoteId).orElseThrow(
            () -> new AssertionError("Expected remote agent to be persisted"));
        Assertions.assertEquals("http://remote:8080", saved.getBaseUrl());
        Assertions.assertTrue(saved.isReachable(),
            "Expected newly registered agent to be reachable");

        var myNetwork = this.agentRepository.findById(this.agentConfig.getId())
            .orElseThrow().getNetwork();
        Assertions.assertNotNull(saved.getNetwork(),
            "Expected remote agent to be assigned to a network");
        Assertions.assertEquals(myNetwork.getId(), saved.getNetwork().getId(),
            "Expected remote agent to share the local agent's network");
    }

    @Test
    @Transactional
    public void assertRegister_existingAgent_updatesBaseUrlAndLiftsThombstone() {
        var remoteId = UUID.randomUUID().toString();
        var existing = new AgentEntity();
        existing.setId(remoteId);
        existing.setDeployedGraphs(new ArrayList<>());
        existing.setBaseUrl("http://old:8080");
        existing.setReachable(false);
        this.agentRepository.save(existing);

        var dto = new Agent();
        dto.setId(remoteId);
        dto.setBaseUrl("http://new:8080");

        this.agentController.register(dto);

        var updated = this.agentRepository.findById(remoteId).orElseThrow();
        Assertions.assertEquals("http://new:8080", updated.getBaseUrl(),
            "Expected baseUrl to be updated on re-registration");
        Assertions.assertTrue(updated.isReachable(),
            "Expected tombstone to be lifted on re-registration");
    }

    @Test
    @Transactional
    public void assertGetInstance_returnsLocalAgent() throws Exception {
        var result = this.agentController.getInstance();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(this.agentConfig.getId(), result.getId());
    }
}
