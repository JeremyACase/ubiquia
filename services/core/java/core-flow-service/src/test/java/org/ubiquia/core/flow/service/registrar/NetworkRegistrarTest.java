package org.ubiquia.core.flow.service.registrar;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.ubiquia.common.model.ubiquia.dto.Network;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.repository.NetworkRepository;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class NetworkRegistrarTest {

    @Autowired
    private NetworkRegistrar networkRegistrar;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    public void assertTryRegister_withNewNetwork_createsEntity() {
        var countBefore = this.networkRepository.count();
        var dto = new Network();

        this.networkRegistrar.tryRegister(dto);

        Assertions.assertEquals(countBefore + 1, this.networkRepository.count());
    }

    @Test
    public void assertTryRegister_withExplicitId_usesProvidedId() {
        var dto = new Network();
        dto.setId("aaaaaaaa-1111-1111-1111-aaaaaaaaaaaa");

        this.networkRegistrar.tryRegister(dto);

        Assertions.assertTrue(this.networkRepository.existsById("aaaaaaaa-1111-1111-1111-aaaaaaaaaaaa"));
    }

    @Test
    public void assertTryRegister_withDuplicateId_doesNotCreateDuplicate() {
        var dto = new Network();
        dto.setId("aaaaaaaa-2222-2222-2222-aaaaaaaaaaaa");

        this.networkRegistrar.tryRegister(dto);
        var countAfterFirst = this.networkRepository.count();

        this.networkRegistrar.tryRegister(dto);

        Assertions.assertEquals(countAfterFirst, this.networkRepository.count());
    }
}
