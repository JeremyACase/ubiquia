package org.ubiquia.core.communication.service.manager.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.implementation.service.builder.NodeEndpointRecordBuilder;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.model.ubiquia.dto.Node;

/**
 * Maintains in-memory registries of node ID/name → {@link Node} for reverse-proxy routing.
 *
 * <p>Driven by graph deployment/teardown lifecycle events from {@link
 * org.ubiquia.core.communication.service.io.flow.DeployedGraphPoller}. Only nodes with
 * {@code communicationServiceSettings.exposeViaCommService == true} are registered.</p>
 */
@Service
public class NodeProxyManager {

    private static final Logger logger = LoggerFactory.getLogger(NodeProxyManager.class);

    private final HashMap<String, Node> proxiedNodesById = new HashMap<>();
    private final HashMap<String, Node> proxiedNodesByName = new HashMap<>();

    @Autowired
    private NodeEndpointRecordBuilder nodeEndpointRecordBuilder;

    /**
     * Registers proxy entries for nodes in a newly deployed graph that opt in to
     * exposure via the communication service.
     *
     * @param graph the deployed graph
     */
    public void tryProcessNewlyDeployedGraph(final Graph graph) {
        var nodesToProxy = graph
            .getNodes()
            .stream()
            .filter(node ->
                Boolean.TRUE.equals(
                    node.getCommunicationServiceSettings().getExposeViaCommService()))
            .toList();

        for (var node : nodesToProxy) {
            logger.info("...node {} is set to be exposed via comm service...", node.getName());
            if (!this.proxiedNodesById.containsKey(node.getId())) {
                logger.info("...registering proxy for node: {}", node.getName());
                var endpoint = this.nodeEndpointRecordBuilder
                    .getBasePathFor(graph.getName(), node.getName());
                logger.info("...proxying base url: {}", endpoint);
                this.proxiedNodesById.put(node.getId(), node);
                this.proxiedNodesByName.put(node.getName(), node);
            }
        }
    }

    /**
     * Unregisters proxy entries for all nodes in a torn-down graph.
     *
     * @param graph the torn-down graph
     */
    public void tryProcessNewlyTornDownGraph(final Graph graph) {
        logger.info("...processing torn down graph: {}", graph.getName());
        for (var node : graph.getNodes()) {
            logger.info("...checking if node {} needs to be torn down...", node.getName());
            if (this.proxiedNodesById.containsKey(node.getId())) {
                logger.info("...unproxying endpoints for node {}...", node.getName());
                this.proxiedNodesById.remove(node.getId());
                this.proxiedNodesByName.remove(node.getName());
            }
        }
    }

    /** Returns the endpoints of all currently registered proxied nodes. */
    public List<String> getRegisteredEndpoints() {
        var endpoints = new ArrayList<String>();
        for (var node : this.proxiedNodesById.values()) {
            endpoints.add(node.getEndpoint());
        }
        return endpoints;
    }

    /**
     * Returns the upstream endpoint for the given node name, or {@code null} if not registered.
     *
     * @param nodeName the node's logical name
     */
    public String getRegisteredEndpointForNodeName(final String nodeName) {
        var node = this.proxiedNodesByName.get(nodeName);
        return node != null ? node.getEndpoint() : null;
    }
}
