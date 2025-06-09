package org.ubiquia.core.communication.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.ubiquia.core.communication.config.FlowServiceConfig;
import org.ubiquia.core.communication.service.manager.flow.AdapterProxyManager;

@RestController
@RequestMapping("/ubiquia/communication-service/reverse-proxy")
public class ReverseProxyController {

    private static final Logger logger = LoggerFactory.getLogger(ReverseProxyController.class);

    @Autowired
    private AdapterProxyManager adapterProxyManager;

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Autowired
    private WebClient webClient;

    @GetMapping("/get-proxied-adapters")
    public List<String> getProxiedAdapters() {
        logger.info("Received request for currently proxied adapters...");
        return this.adapterProxyManager.getRegisteredEndpoints();
    }

    @RequestMapping(value = "/{adapterName}/**",
        method = {RequestMethod.GET,
            RequestMethod.POST,
            RequestMethod.PUT}
    )
    public void proxyToAdapter(
        @PathVariable String adapterName,
        HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        var registeredEndpoint = this.adapterProxyManager.getRegisteredEndpointFor(adapterName);
        if (Objects.nonNull(registeredEndpoint)) {

            var cleanedPath = request.getRequestURI()
                .replace("/ubiquia/communication-service/reverse-proxy", "")
                .replace(adapterName, "")
                .replace("/", "");

            var targetUrl = this.flowServiceConfig.getUrl()
                + ":"
                + this.flowServiceConfig.getPort()
                + "/"
                + registeredEndpoint
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
                + adapterName);
        }
    }
}
