package org.ubiquia.core.communication.service.manager.flow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ubiquia.common.library.implementation.service.builder.NodeEndpointRecordBuilder;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.embeddable.CommunicationServiceSettings;

@ExtendWith(MockitoExtension.class)
class NodeProxyManagerTest {

    @Mock
    private NodeEndpointRecordBuilder nodeEndpointRecordBuilder;

    @InjectMocks
    private NodeProxyManager nodeProxyManager;

    private Graph graph;
    private Node node;

    @BeforeEach
    void setUp() {
        graph = new Graph();
        graph.setName("test-graph");
        graph.setNodes(new ArrayList<>());

        node = new Node();
        node.setId(UUID.randomUUID().toString());
        node.setName("test-node");
        node.setEndpoint("/test/endpoint");
        var settings = new CommunicationServiceSettings();
        settings.setExposeViaCommService(true);
        node.setCommunicationServiceSettings(settings);
        graph.getNodes().add(node);
    }

    @Test
    @DisplayName("Deploy: node is reachable by name after registration")
    void deploy_registersNodeByName() {
        this.nodeProxyManager.tryProcessNewlyDeployedGraph(graph);

        assertThat(this.nodeProxyManager.getRegisteredEndpointForNodeName(node.getName()))
            .isEqualTo(node.getEndpoint());
    }

    @Test
    @DisplayName("Deploy: endpoint appears in getRegisteredEndpoints")
    void deploy_endpointAppearsInList() {
        this.nodeProxyManager.tryProcessNewlyDeployedGraph(graph);

        assertThat(this.nodeProxyManager.getRegisteredEndpoints())
            .contains(node.getEndpoint());
    }

    @Test
    @DisplayName("Deploy: node with exposeViaCommService=false is not registered")
    void deploy_skipsNodeNotFlaggedForExposure() {
        node.getCommunicationServiceSettings().setExposeViaCommService(false);
        this.nodeProxyManager.tryProcessNewlyDeployedGraph(graph);

        assertThat(this.nodeProxyManager.getRegisteredEndpointForNodeName(node.getName())).isNull();
        assertThat(this.nodeProxyManager.getRegisteredEndpoints()).isEmpty();
    }

    @Test
    @DisplayName("Teardown: node is no longer reachable by name")
    void teardown_deregistersNodeByName() {
        this.nodeProxyManager.tryProcessNewlyDeployedGraph(graph);
        this.nodeProxyManager.tryProcessNewlyTornDownGraph(graph);

        assertThat(this.nodeProxyManager.getRegisteredEndpointForNodeName(node.getName())).isNull();
    }

    @Test
    @DisplayName("Teardown: endpoint removed from getRegisteredEndpoints")
    void teardown_endpointRemovedFromList() {
        this.nodeProxyManager.tryProcessNewlyDeployedGraph(graph);
        this.nodeProxyManager.tryProcessNewlyTornDownGraph(graph);

        assertThat(this.nodeProxyManager.getRegisteredEndpoints())
            .doesNotContain(node.getEndpoint());
    }
}
