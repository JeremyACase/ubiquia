package org.ubiquia.core.communication.controller.flow;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.core.communication.service.manager.flow.NodeProxyManager;

/**
 * Reverse proxy controller for adapter endpoints.
 *
 * <p>
 * This controller exposes a generic reverse-proxy route under
 * {@code /ubiquia/communication-service/adapter-reverse-proxy}. Requests to
 * {@code /{adapterName}/**} are forwarded to a target adapter endpoint that is
 * dynamically looked up via {@link NodeProxyManager}. The final target URL is
 * constructed from the configured {@link FlowServiceConfig} host/port and the
 * registered adapter endpoint.
 * </p>
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>Supported methods: {@code GET}, {@code POST}, {@code PUT}.</li>
 *   <li>All inbound headers are copied to the outbound request (subject to
 *       {@link HttpURLConnection} handling of restricted headers).</li>
 *   <li>For {@code POST}/{@code PUT}, the request body is streamed to the target.</li>
 *   <li>The downstream status code and headers are propagated back to the client,
 *       and the response body is streamed back.</li>
 * </ul>
 *
 * <h3>Implementation notes & limitations</h3>
 * <ul>
 *   <li>Networking is performed with {@link HttpURLConnection} and blocking I/O;
 *       this controller is not reactive.</li>
 *   <li>Path derivation removes the controller prefix and the {@code adapterName}.
 *       The current implementation also removes {@code '/'} characters when building
 *       {@code cleanedPath}; adjust if preserving sub-path separators is desired.</li>
 *   <li>Query parameters are not currently forwarded. If required, append the original
 *       query string to the target URL.</li>
 *   <li>On non-2xx responses, {@link HttpURLConnection#getInputStream()} may throw;
 *       consider reading {@code getErrorStream()} for error bodies.</li>
 * </ul>
 *
 * @see NodeProxyManager
 * @see FlowServiceConfig
 */
@RestController
@RequestMapping("/ubiquia/core/communication-service/node-reverse-proxy")
public class DeployedNodeProxyController {

    private static final Logger logger = LoggerFactory.getLogger(DeployedNodeProxyController.class);

    /**
     * Registry for dynamically discovered/registered adapter endpoints.
     */
    @Autowired
    private NodeProxyManager nodeProxyManager;

    /**
     * Base host/port configuration used to build the adapter target URL.
     */
    @Autowired
    private FlowServiceConfig flowServiceConfig;

    /**
     * Autowired {@link WebClient}. Currently unused by this controller, which relies on
     * {@link HttpURLConnection}; kept for parity with other controllers and potential future use.
     */
    @Autowired
    private WebClient webClient;

    /**
     * Returns the set of adapter endpoint paths currently registered for proxying.
     *
     * <p>
     * This is primarily a diagnostics/introspection endpoint to verify what the
     * {@link NodeProxyManager} has registered.
     * </p>
     *
     * @return a list of registered adapter endpoint strings
     */
    @GetMapping("/get-proxied-urls")
    public List<String> getProxiedUrls() {
        logger.info("Received request for currently proxied urls...");
        return this.nodeProxyManager.getRegisteredEndpoints();
    }

    /**
     * Reverse-proxies a request addressed to {@code /{adapterName}/**} to the adapter's
     * registered endpoint behind the Flow service.
     *
     * <p>
     * The method:
     * </p>
     * <ol>
     *   <li>Looks up the adapter's registered endpoint using {@link NodeProxyManager}.</li>
     *   <li>Builds a target URL using {@link FlowServiceConfig#getUrl()} and {@link FlowServiceConfig#getPort()}.</li>
     *   <li>Copies inbound headers to the outbound connection.</li>
     *   <li>For {@code POST}/{@code PUT}, streams the request body to the target.</li>
     *   <li>Propagates the downstream status, headers, and body to the client.</li>
     * </ol>
     *
     * <p><strong>Note:</strong> the current path cleaning removes all {@code '/'} characters from
     * the trailing path. If your adapters require hierarchical paths, consider preserving
     * separators when computing {@code cleanedPath}.</p>
     *
     * @param nodeName the logical adapter identifier used to resolve the target endpoint
     * @param request     the incoming servlet request from the client
     * @param response    the servlet response to write the proxied result into
     * @throws IOException              if an I/O error occurs while forwarding the request/response
     * @throws IllegalArgumentException if no adapter is registered under the given {@code adapterName}
     */
    @RequestMapping(
        value = "/{nodeName}/**",
        method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT}
    )
    public void proxyToAdapter(
        @PathVariable String nodeName,
        HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        var registeredEndpoint = this.nodeProxyManager.getRegisteredEndpointFor(nodeName);
        if (Objects.nonNull(registeredEndpoint)) {

            var cleanedPath = request.getRequestURI()
                .replace("/ubiquia/communication-service/adapter-reverse-proxy", "")
                .replace(nodeName, "")
                .replace("/", "");

            var targetUrl = this.flowServiceConfig.getUrl()
                + ":"
                + this.flowServiceConfig.getPort()
                + "/"
                + registeredEndpoint
                + "/"
                + cleanedPath;

            logger.debug("Reverse proxying to URL {}...", targetUrl);

            // Create a connection to the target
            var url = new URL(targetUrl);
            var connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(request.getMethod());

            // Copy headers
            Collections.list(request.getHeaderNames()).forEach(headerName -> {
                Collections.list(request.getHeaders(headerName)).forEach(headerValue -> {
                    connection.setRequestProperty(headerName, headerValue);
                });
            });

            // Copy body if needed
            if ("POST".equalsIgnoreCase(request.getMethod())
                || "PUT".equalsIgnoreCase(request.getMethod())) {
                connection.setDoOutput(true);
                IOUtils.copy(request.getInputStream(), connection.getOutputStream());
            }

            // Forward response status and headers
            response.setStatus(connection.getResponseCode());
            connection.getHeaderFields().forEach((headerName, headerValues) -> {
                if (headerName != null) {
                    headerValues.forEach(headerValue -> response.addHeader(headerName, headerValue));
                }
            });

            // Forward response body
            try (var adapterStream = connection.getInputStream();
                 var clientStream = response.getOutputStream()) {

                var buffer = new byte[8192];

                int bytesRead;
                while ((bytesRead = adapterStream.read(buffer)) != -1) {
                    clientStream.write(buffer, 0, bytesRead);
                }
                clientStream.flush();
            }
        } else {
            throw new IllegalArgumentException("ERROR: No adapter registered with name: "
                + nodeName);
        }
    }
}
