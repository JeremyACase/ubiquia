package org.ubiquia.common.library.implementation.service.builder;


import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.record.EndpointRecord;

/** Service for constructing endpoint URIs and records for node services within a graph. */
@Service
public class NodeEndpointRecordBuilder {

    /**
     * Build a URI targeting the given node's HTTP endpoint.
     *
     * @param node The node whose endpoint URI is needed.
     * @return A fully formed {@link URI} for the node.
     * @throws URISyntaxException If the constructed URI string is malformed.
     */
    public URI getComponentUriFrom(final Node node)
        throws URISyntaxException {

        return new URI(
            "http",
            null,
            node.getTargetComponent().getName().toLowerCase(),
            node.getTargetComponent().getPort(),
            node.getEndpoint(),
            null,
            null
        );
    }

    /**
     * Build the back-pressure polling endpoint record for a node.
     *
     * @param graphName The name of the graph.
     * @param nodeName  The name of the node.
     * @return An {@link EndpointRecord} for the back-pressure endpoint.
     */
    public EndpointRecord getBackpressureEndpointFor(
        final String graphName,
        final String nodeName) {

        var path = this.getBasePathFor(graphName, nodeName) + "/back-pressure";
        return new EndpointRecord(path, RequestMethod.GET);
    }

    /**
     * Build the push endpoint record for a node.
     *
     * @param graphName The name of the graph.
     * @param nodeName  The name of the node.
     * @return An {@link EndpointRecord} for the push endpoint.
     */
    public EndpointRecord getPushEndpointFor(final String graphName, final String nodeName) {
        var path = this.getBasePathFor(graphName, nodeName) + "/push";
        return new EndpointRecord(path, RequestMethod.POST);
    }

    /**
     * Build the queue-peek endpoint record for a node.
     *
     * @param graphName The name of the graph.
     * @param nodeName  The name of the node.
     * @return An {@link EndpointRecord} for the peek endpoint.
     */
    public EndpointRecord getPeekEndpointFor(final String graphName, final String nodeName) {
        var path = this.getBasePathFor(graphName, nodeName) + "/queue/peek";
        return new EndpointRecord(path, RequestMethod.GET);
    }

    /**
     * Build the queue-pop endpoint record for a node.
     *
     * @param graphName The name of the graph.
     * @param nodeName  The name of the node.
     * @return An {@link EndpointRecord} for the pop endpoint.
     */
    public EndpointRecord getPopEndpointFor(final String graphName, final String nodeName) {
        var path = this.getBasePathFor(graphName, nodeName) + "/queue/pop";
        return new EndpointRecord(path, RequestMethod.GET);
    }

    /**
     * Build the base URL path for a node within a graph.
     *
     * @param graphName The name of the graph.
     * @param nodeName  The name of the node.
     * @return The base path string.
     */
    public String getBasePathFor(final String graphName, final String nodeName) {
        return "ubiquia/core-flow-service/"
            + graphName.toLowerCase()
            + "/node/"
            + nodeName.toLowerCase();
    }
}