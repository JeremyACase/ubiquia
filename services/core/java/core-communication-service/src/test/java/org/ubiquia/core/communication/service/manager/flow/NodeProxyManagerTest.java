package org.ubiquia.core.communication.service.manager.flow;

import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.embeddable.CommunicationServiceSettings;
import org.ubiquia.core.communication.dummy.DummyFactory;
import org.ubiquia.core.communication.service.io.flow.DeployedGraphPoller;

@SpringBootTest
class NodeProxyManagerTest {

    @Autowired
    private DummyFactory dummyFactory;

    @MockitoBean
    private DeployedGraphPoller deployedGraphPoller;

    @Autowired
    private NodeProxyManager nodeProxyManager;

    @Test
    public void assertDeploysNode_isValid() {

        var graph = this.dummyFactory.generateGraph();

        var node = this.dummyFactory.generateNode();
        node.setCommunicationServiceSettings(new CommunicationServiceSettings());
        node.getCommunicationServiceSettings().setExposeViaCommService(true);
        graph.getNodes().add(node);

        this.nodeProxyManager.tryProcessNewlyDeployedGraph(graph);

        @SuppressWarnings("unchecked")
        var map = (HashMap<String, Node>) ReflectionTestUtils
            .getField(this.nodeProxyManager, "proxiedNodes");

        Assertions.assertTrue(map.containsKey(node.getId()));
    }

    @Test
    public void assertContainsEndpoint_isValid() {

        var graph = this.dummyFactory.generateGraph();

        var node = this.dummyFactory.generateNode();
        node.setCommunicationServiceSettings(new CommunicationServiceSettings());
        node.getCommunicationServiceSettings().setExposeViaCommService(true);
        graph.getNodes().add(node);

        this.nodeProxyManager.tryProcessNewlyDeployedGraph(graph);

        var endpoints = this.nodeProxyManager.getRegisteredEndpoints();
        Assertions.assertTrue(endpoints.contains(node.getEndpoint()));
    }

    @Test
    public void assertTearsDownNode_isValid() {

        var graph = this.dummyFactory.generateGraph();

        var node = this.dummyFactory.generateNode();
        node.setCommunicationServiceSettings(new CommunicationServiceSettings());
        node.getCommunicationServiceSettings().setExposeViaCommService(true);
        graph.getNodes().add(node);

        this.nodeProxyManager.tryProcessNewlyDeployedGraph(graph);
        this.nodeProxyManager.tryProcessNewlyTornDownGraph(graph);

        @SuppressWarnings("unchecked")
        var map = (HashMap<String, Node>) ReflectionTestUtils
            .getField(this.nodeProxyManager, "proxiedNodes");

        Assertions.assertFalse(map.containsKey(node.getId()));
    }

    @Test
    public void assertContainsDoesNotContainEndpoint_isValid() {

        var graph = this.dummyFactory.generateGraph();

        var node = this.dummyFactory.generateNode();
        node.setCommunicationServiceSettings(new CommunicationServiceSettings());
        node.getCommunicationServiceSettings().setExposeViaCommService(true);
        graph.getNodes().add(node);

        this.nodeProxyManager.tryProcessNewlyDeployedGraph(graph);
        this.nodeProxyManager.tryProcessNewlyTornDownGraph(graph);

        var endpoints = this.nodeProxyManager.getRegisteredEndpoints();
        Assertions.assertFalse(endpoints.contains(node.getEndpoint()));
    }
}
