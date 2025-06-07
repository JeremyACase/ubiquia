package org.ubiquia.core.communication.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.ubiquia.core.communication.service.manager.flow.AdapterProxyManager;

@RestController
@RequestMapping("/proxy")
public class ReverseProxyController {

    private static final Logger logger = LoggerFactory.getLogger(ReverseProxyController.class);

    @Autowired
    private AdapterProxyManager adapterProxyManager;

    @Autowired
    private WebClient webClient;

    @RequestMapping(value = "/{adapterName}/**",
        method = {RequestMethod.GET,
            RequestMethod.POST,
            RequestMethod.PUT}
    )
    public void proxyToAdapter(
        @PathVariable String adapterName,
        HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        var targetBaseUrl = adapterProxyManager.getProxiedAdapterMap().get(adapterName).get(0).toString();
        var path = request.getRequestURI().replaceFirst("/proxy/" + adapterName, "");
        var targetUrl = targetBaseUrl + path + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

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

        // Forward response
        response.setStatus(connection.getResponseCode());
        connection.getHeaderFields().forEach((headerName, headerValues) -> {
            if (headerName != null) {
                headerValues.forEach(headerValue -> response.addHeader(headerName, headerValue));
            }
        });
    }
}
