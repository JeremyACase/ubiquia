package org.ubiquia.core.flow.service.mapper;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.common.library.implementation.service.mapper.DomainOntologyDtoMapper;
import org.ubiquia.common.model.ubiquia.entity.SyncEntity;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.DomainOntologyController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.repository.DomainOntologyRepository;
import org.ubiquia.core.flow.repository.SyncRepository;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SyncMappingTest {

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private DomainOntologyController domainOntologyController;

    @Autowired
    private DomainOntologyRepository domainOntologyRepository;

    @Autowired
    private DomainOntologyDtoMapper domainOntologyDtoMapper;

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private SyncRepository syncRepository;

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
    public void assertSyncsAreMapped_isValid() throws Exception {

        var domainOntology = this.dummyFactory.generateDomainOntology();
        var registered = this.domainOntologyController.register(domainOntology);

        var agentEntity = this.agentRepository
            .findById(this.agentConfig.getId())
            .orElseThrow();

        var entity = this.domainOntologyRepository
            .findById(registered.getId())
            .orElseThrow();

        var syncEntity = new SyncEntity();
        syncEntity.setModel(entity);
        syncEntity.setSourceAgent(agentEntity);
        this.syncRepository.save(syncEntity);

        // Flush writes to DB and clear the first-level cache so the re-fetch
        // returns a fresh instance with the syncs collection populated.
        this.entityManager.flush();
        this.entityManager.clear();

        entity = this.domainOntologyRepository
            .findById(entity.getId())
            .orElseThrow();

        var dto = this.domainOntologyDtoMapper.map(entity);

        // Syncs are no longer hydrated by the mapper to avoid unbounded lazy-loading.
        // Callers should query /sync/query/params?sourceAgent.id=<id> instead.
        Assertions.assertNull(dto.getSyncs(), "Expected syncs to be null — mapper must not hydrate the lazy collection");

        // Confirm the SyncEntity was actually persisted correctly.
        Assertions.assertEquals(1, this.syncRepository.count(), "Expected exactly one SyncEntity in the database");
    }
}
