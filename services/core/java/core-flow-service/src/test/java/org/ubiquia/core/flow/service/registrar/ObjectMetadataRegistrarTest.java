package org.ubiquia.core.flow.service.registrar;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.model.ubiquia.dto.Agent;
import org.ubiquia.common.model.ubiquia.dto.ObjectMetadata;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.repository.ObjectMetadataRepository;

/** Test class for ObjectMetadataRegistrarTest. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ObjectMetadataRegistrarTest {

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private ObjectMetadataRegistrar objectMetadataRegistrar;

    @Autowired
    private ObjectMetadataRepository objectMetadataRepository;

    @Autowired
    private TestHelper testHelper;

    /** Sets up test fixtures. */
    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    public void assertTryRegister_withNullAgentReference_throwsIllegalArgument() {
        var dto = new ObjectMetadata();
        dto.setBucketName("test-bucket");

        Assertions.assertThrows(IllegalArgumentException.class,
            () -> this.objectMetadataRegistrar.tryRegister(dto));
    }

    @Test
    public void assertTryRegister_withAgentNotFound_throwsIllegalArgument() {
        var agent = new Agent();
        agent.setId("ffffffff-0000-0000-0000-000000000000");
        var dto = new ObjectMetadata();
        dto.setUbiquiaAgent(agent);
        dto.setBucketName("test-bucket");

        Assertions.assertThrows(IllegalArgumentException.class,
            () -> this.objectMetadataRegistrar.tryRegister(dto));
    }

    @Test
    public void assertTryRegister_withValidAgent_createsEntity() {
        var agent = new Agent();
        agent.setId(this.agentConfig.getId());
        var dto = new ObjectMetadata();
        dto.setUbiquiaAgent(agent);
        dto.setBucketName("test-bucket");
        dto.setContentType("application/json");
        dto.setOriginalFilename("data.json");
        dto.setSize(1024L);

        this.objectMetadataRegistrar.tryRegister(dto);

        Assertions.assertEquals(1L, this.objectMetadataRepository.count());
    }

    @Test
    public void assertTryRegister_withDuplicateId_doesNotCreateDuplicate() {
        var agent = new Agent();
        agent.setId(this.agentConfig.getId());
        var dto = new ObjectMetadata();
        dto.setId("aaaaaaaa-3333-3333-3333-aaaaaaaaaaaa");
        dto.setUbiquiaAgent(agent);
        dto.setBucketName("test-bucket");

        this.objectMetadataRegistrar.tryRegister(dto);
        var countAfterFirst = this.objectMetadataRepository.count();

        this.objectMetadataRegistrar.tryRegister(dto);

        Assertions.assertEquals(countAfterFirst, this.objectMetadataRepository.count());
    }
}
