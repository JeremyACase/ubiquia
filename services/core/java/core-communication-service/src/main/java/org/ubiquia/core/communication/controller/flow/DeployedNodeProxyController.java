package org.ubiquia.core.communication.controller.flow;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.core.communication.controller.AbstractReverseProxyController;
import org.ubiquia.core.communication.service.manager.flow.NodeProxyManager;

/**
 * Reverse proxy controller for deployed node endpoints.
 *
 * <p>Forwards requests under {@code /ubiquia/core-communication-service/{graph}/node/{node}/**}
 * to the flow service at {@code /ubiquia/core-flow-service/{graph}/node/{node}/**}.</p>
 */
@RestController
@RequestMapping("/ubiquia/core-communication-service")
public class DeployedNodeProxyController extends AbstractReverseProxyController {

    private static final Logger logger = LoggerFactory.getLogger(DeployedNodeProxyController.class);

    @Autowired
    private NodeProxyManager nodeProxyManager;

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    /** Returns the list of upstream node URLs currently registered for proxying. */
    @GetMapping("/node/get-proxied-urls")
    public List<String> getProxiedUrls() {
        logger.info("Received request for currently proxied node urls...");
        return this.nodeProxyManager.getRegisteredEndpoints();
    }

    /** Proxies GET, POST, and PUT requests to the registered upstream node endpoint. */
    @RequestMapping(
        value = "/{graph}/node/{node}/**",
        method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT}
    )
    public void proxyToNode(
        @PathVariable final String graph,
        @PathVariable final String node,
        final HttpServletRequest request,
        final HttpServletResponse response) throws IOException {
        this.executeProxy(request, response, Map.of("graph", graph, "node", node));
    }

    @Override
    protected boolean rewritesHtmlAndCss() {
        return false;
    }

    @Override
    protected UpstreamResolution resolveUpstream(
        final HttpServletRequest request,
        final String remainder,
        final boolean isStaticAsset,
        final Map<String, String> pathVars) {

        var node = pathVars.get("node");
        var graph = pathVars.get("graph");

        var registered = this.nodeProxyManager.getRegisteredEndpointForNodeName(node);
        if (Objects.isNull(registered)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "No node registered with name: " + node);
        }

        var base = UriComponentsBuilder
            .fromHttpUrl(this.flowServiceConfig.getBaseUrl())
            .path("/ubiquia/core-flow-service/"
                + graph.toLowerCase() + "/node/" + node.toLowerCase())
            .build(true).toUri();

        return UpstreamResolution.of(base, false);
    }
}
