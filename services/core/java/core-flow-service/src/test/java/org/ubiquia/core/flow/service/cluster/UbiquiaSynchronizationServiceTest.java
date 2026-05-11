package org.ubiquia.core.flow.service.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.List;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.View;
import org.jgroups.stack.IpAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.DomainOntologyController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.repository.SyncRepository;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
    "ubiquia.cluster.flow-service.sync.enabled=true",
    "server.port=8080"
})
public class UbiquiaSynchronizationServiceTest {

    @MockBean
    private FlowClusterService flowClusterService;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private UbiquiaSynchronizationService ubiquiaSynchronizationService;

    @Autowired
    private DomainOntologyController domainOntologyController;

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private SyncRepository syncRepository;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    public void assertSynchronize_withNullChannel_skipsSync() {
        when(flowClusterService.getChannel()).thenReturn(null);

        this.ubiquiaSynchronizationService.synchronize();

        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    public void assertSynchronize_withNullView_skipsSync() {
        var mockChannel = mock(JChannel.class);
        when(mockChannel.getView()).thenReturn(null);
        when(flowClusterService.getChannel()).thenReturn(mockChannel);

        this.ubiquiaSynchronizationService.synchronize();

        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    public void assertSynchronize_withPeerButNoEntitiesNeedingSync_skipsPost() throws Exception {
        var mockChannel = mock(JChannel.class);
        var mockView = mock(View.class);
        var localAddress = new IpAddress(InetAddress.getByName("127.0.0.1"), 7800);
        var peerAddress = new IpAddress(InetAddress.getByName("192.168.1.100"), 7800);

        when(flowClusterService.getChannel()).thenReturn(mockChannel);
        when(mockChannel.getView()).thenReturn(mockView);
        when(mockChannel.getAddress()).thenReturn(localAddress);
        when(mockView.getMembers()).thenReturn(List.of(peerAddress));
        when(mockChannel.down(isA(Event.class))).thenReturn(peerAddress);

        this.ubiquiaSynchronizationService.synchronize();

        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    public void assertSynchronize_withPeerAndEntitiesNeedingSync_postsAndCreatesSyncRecords()
        throws Exception {

        var domainOntology = this.dummyFactory.generateDomainOntology();
        this.domainOntologyController.register(domainOntology);

        var mockChannel = mock(JChannel.class);
        var mockView = mock(View.class);
        var localAddress = new IpAddress(InetAddress.getByName("127.0.0.1"), 7800);
        var peerAddress = new IpAddress(InetAddress.getByName("192.168.1.100"), 7800);

        when(flowClusterService.getChannel()).thenReturn(mockChannel);
        when(mockChannel.getView()).thenReturn(mockView);
        when(mockChannel.getAddress()).thenReturn(localAddress);
        when(mockView.getMembers()).thenReturn(List.of(peerAddress));
        when(mockChannel.down(isA(Event.class))).thenReturn(peerAddress);

        this.ubiquiaSynchronizationService.synchronize();

        verify(restTemplate, atLeast(1)).postForEntity(anyString(), any(), eq(Void.class));
        Assertions.assertTrue(
            this.syncRepository.count() > 0,
            "Expected SyncEntity records to be created after synchronization");
    }
}
