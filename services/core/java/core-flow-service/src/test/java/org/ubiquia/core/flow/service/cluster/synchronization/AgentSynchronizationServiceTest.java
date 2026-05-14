package org.ubiquia.core.flow.service.cluster.synchronization;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.transaction.Transactional;
import java.util.List;
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

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AgentSynchronizationServiceTest {

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private AgentSynchronizationService agentSynchronizationService;

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    public void assertSync_withEmptyPeerList_doesNotPost() {
        this.agentSynchronizationService.sync(List.of());

        verify(this.restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    public void assertSync_withoutBaseUrl_doesNotPost() {
        // The local agent has no baseUrl by default in the test config.
        this.agentSynchronizationService.sync(List.of("http://peer:8080"));

        verify(this.restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    @Transactional
    public void assertSync_withBaseUrl_postsToEachPeer() {
        var myAgent = this.agentRepository.findById(this.agentConfig.getId()).orElseThrow();
        myAgent.setBaseUrl("http://self:8080");
        this.agentRepository.save(myAgent);

        this.agentSynchronizationService.sync(List.of("http://peer-1:8080", "http://peer-2:8080"));

        verify(this.restTemplate, times(2)).postForEntity(anyString(), any(), eq(Void.class));
    }
}
