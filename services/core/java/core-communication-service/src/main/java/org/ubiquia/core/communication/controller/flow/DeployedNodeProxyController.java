package org.ubiquia.core.communication.controller.flow;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.core.communication.service.manager.flow.NodeProxyManager;

/**
 * Reverse proxy controller for deployed node endpoints.
 *
 * <p>
 * Requests to {@code /ubiquia/core-communication-service/{graph}/{node}/**} are forwarded
 * to the corresponding endpoint on the flow service at
 * {@code /ubiquia/core-flow-service/{graph}/{node}/**}.
 * </p>
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>Supported methods: {@code GET}, {@code POST}, {@code PUT}.</li>
 *   <li>Hop-by-hop headers are filtered from both request and response.</li>
 *   <li>Request body is streamed for {@code POST}/{@code PUT} when present.</li>
 *   <li>Query parameters are forwarded to the upstream target.</li>
 *   <li>Returns {@code 400 Bad Request} if the node is not registered.</li>
 *   <li>Error stream is forwarded for 4xx/5xx upstream responses.</li>
 * </ul>
 *
 * @see NodeProxyManager
 * @see FlowServiceConfig
 */
@RestController
@RequestMapping("/ubiquia/core-communication-service")
public class DeployedNodeProxyController {

    private static final Logger logger = LoggerFactory.getLogger(DeployedNodeProxyController.class);

    private final Set<String> hopByHopHeaders;

    @Autowired
    private NodeProxyManager nodeProxyManager;

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    public DeployedNodeProxyController() {
        this.hopByHopHeaders = new HashSet<>(Arrays.asList(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailer", "transfer-encoding", "upgrade",
            "host", "content-length", "content-encoding", "accept-encoding"
        ));
    }

    /**
     * Returns the set of node endpoint paths currently registered for proxying.
     *
     * @return a list of registered node endpoint strings
     */
    @GetMapping("/node/get-proxied-urls")
    public List<String> getProxiedUrls() {
        logger.info("Received request for currently proxied node urls...");
        return this.nodeProxyManager.getRegisteredEndpoints();
    }

    /**
     * Reverse-proxies a request to the flow service node endpoint.
     *
     * <p>
     * Resolves the node registration, derives the tail path after {@code {node}}, constructs
     * the upstream target at {@code /ubiquia/core-flow-service/{graph}/{node}/{tail}}, and
     * proxies the request/response with selective header copying.
     * </p>
     *
     * @param graph    the graph name the node belongs to
     * @param node     the node name used to validate registration
     * @param request  the incoming servlet request
     * @param response the servlet response to write the proxied result into
     * @throws IOException             on I/O errors while streaming request/response bodies
     * @throws ResponseStatusException with {@code 400 Bad Request} if the node is unknown
     */
    @RequestMapping(
        value = "/{graph}/node/{node}/**",
        method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT}
    )
    public void proxyToNode(
        @PathVariable final String graph,
        @PathVariable final String node,
        final HttpServletRequest request,
        final HttpServletResponse response) throws IOException {

        var registeredEndpoint = this.nodeProxyManager.getRegisteredEndpointForNodeName(node);
        if (registeredEndpoint == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "No node registered with name: " + node);
        }

        // Extract the tail path after /{graph}/{node}
        var pathWithin = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        var bestPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        var tail = new AntPathMatcher().extractPathWithinPattern(bestPattern, pathWithin);
        var remainder = (tail == null) ? "" : tail;

        // Build the upstream URL: http://<flow-service>:<port>/ubiquia/core-flow-service/{graph}/node/{node}/{tail}
        var serviceBase = this.flowServiceConfig.getUrl() + ":" + this.flowServiceConfig.getPort();
        var targetUrl = serviceBase
            + "/ubiquia/core-flow-service/"
            + graph.toLowerCase()
            + "/node/"
            + node.toLowerCase()
            + (remainder.isEmpty() ? "" : "/" + remainder);

        if (request.getQueryString() != null) {
            targetUrl += "?" + request.getQueryString();
        }

        logger.debug("Reverse proxying {} -> {}", request.getMethod(), targetUrl);

        var conn = (HttpURLConnection) new URL(targetUrl).openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod(request.getMethod());
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(60_000);

        // Copy request headers, skipping hop-by-hop
        var headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            var h = headerNames.nextElement();
            if (h != null && !this.hopByHopHeaders.contains(h.toLowerCase(Locale.ROOT))) {
                var vals = request.getHeaders(h);
                while (vals.hasMoreElements()) {
                    conn.addRequestProperty(h, vals.nextElement());
                }
            }
        }

        // Stream request body for POST/PUT
        if ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod())) {
            var hasBody = request.getContentLengthLong() > 0
                || request.getHeader("Transfer-Encoding") != null;
            if (hasBody) {
                conn.setDoOutput(true);
                try (var in = request.getInputStream()) {
                    IOUtils.copy(in, conn.getOutputStream());
                    conn.getOutputStream().flush();
                }
            }
        }

        int status;
        try {
            status = conn.getResponseCode();
        } catch (IOException e) {
            status = HttpStatus.BAD_GATEWAY.value();
        }
        response.setStatus(status);

        // Copy response headers, skipping hop-by-hop
        var upstreamHeaders = conn.getHeaderFields();
        if (upstreamHeaders != null) {
            for (var entry : upstreamHeaders.entrySet()) {
                var name = entry.getKey();
                var values = entry.getValue();
                if (name != null
                    && !this.hopByHopHeaders.contains(name.toLowerCase(Locale.ROOT))
                    && values != null) {
                    for (var v : values) {
                        response.addHeader(name, v);
                    }
                }
            }
        }

        // Stream response body
        try (var out = response.getOutputStream()) {
            if (status >= 400) {
                var err = conn.getErrorStream();
                if (err != null) {
                    IOUtils.copy(err, out);
                    out.flush();
                }
            } else {
                try (var upstream = conn.getInputStream()) {
                    IOUtils.copy(upstream, out);
                    out.flush();
                }
            }
        }
    }
}
