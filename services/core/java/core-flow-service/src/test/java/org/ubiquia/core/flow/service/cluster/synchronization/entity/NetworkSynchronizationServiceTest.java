package org.ubiquia.core.flow.service.cluster.synchronization.entity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.repository.SyncRepository;

/** Test class for NetworkSynchronizationServiceTest. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class NetworkSynchronizationServiceTest {

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private NetworkSynchronizationService networkSynchronizationService;

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private SyncRepository syncRepository;

    @Autowired
    private TestHelper testHelper;

    /** Sets up test fixtures. */
    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    public void assertSync_withEmptyPeerList_doesNotPost() {
        var agentEntity = this.agentRepository.findById(this.agentConfig.getId()).orElseThrow();

        this.networkSynchronizationService.sync(List.of(), agentEntity);

        verify(this.restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    public void assertSync_withNetworkEntityAndPeerSuccess_postsAndCreatesSyncEntity() {
        var agentEntity = this.agentRepository.findById(this.agentConfig.getId()).orElseThrow();

        this.networkSynchronizationService.sync(List.of("http://peer:8080"), agentEntity);

        verify(this.restTemplate, atLeastOnce())
            .postForEntity(anyString(), any(), eq(Void.class));
        Assertions.assertTrue(this.syncRepository.count() > 0,
            "Expected SyncEntity records after successful sync");
    }

    @Test
    public void assertSync_withNetworkEntityAndPeerFailure_doesNotCreateSyncEntity() {
        doThrow(new RuntimeException("connection refused"))
            .when(this.restTemplate).postForEntity(anyString(), any(), any());

        var agentEntity = this.agentRepository.findById(this.agentConfig.getId()).orElseThrow();

        this.networkSynchronizationService.sync(List.of("http://peer:8080"), agentEntity);

        Assertions.assertEquals(0L, this.syncRepository.count(),
            "Expected no SyncEntity records when peer POST fails");
    }
}
