package org.ubiquia.core.communication.controller.flow;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.ubiquia.core.communication.service.manager.flow.NodeProxyManager;

@SpringBootTest(properties = {
    "spring.task.scheduling.enabled=false",
    "ubiquia.flow.service.url=http://flow-host",
    "ubiquia.flow.service.port=8080",
    "ubiquia.flow.service.poll-frequency-milliseconds=60000"
})
@AutoConfigureMockMvc
class DeployedNodeProxyControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private Environment env;

    @MockitoBean private NodeProxyManager nodeProxyManager;

    private String flowUrl() {
        return env.getRequiredProperty("ubiquia.flow.service.url");
    }

    private int flowPort() {
        return Integer.parseInt(env.getRequiredProperty("ubiquia.flow.service.port"));
    }

    @Test
    void getProxiedUrls_returnsRegisteredEndpoints() throws Exception {
        when(nodeProxyManager.getRegisteredEndpoints()).thenReturn(List.of("a/b", "c/d"));

        var base = "/ubiquia/core/communication-service/node-reverse-proxy";

        mockMvc.perform(get(base + "/get-proxied-urls"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[*]").value(containsInAnyOrder("a/b", "c/d")));

        verify(nodeProxyManager).getRegisteredEndpoints();
    }

    @Test
    void proxyToNode_get_forwardsStatusHeadersAndBody() throws Exception {
        var nodeName = "nodeA";
        when(nodeProxyManager.getRegisteredEndpointFor(nodeName)).thenReturn("graph-x/node-a");

        var connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode()).thenReturn(200);
        when(connection.getHeaderFields()).thenReturn(Map.of(
            "X-Downstream", List.of("yes"),
            "Content-Type", List.of("text/plain")
        ));
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream("OK".getBytes()));

        var capturedTargetUrl = new StringBuilder();

        try (var mockedUrl =
                 mockConstruction(URL.class, (mock, context) -> {
                     capturedTargetUrl.append(context.arguments().get(0));
                     when(mock.openConnection()).thenReturn(connection);
                 })) {

            var base = "/ubiquia/core/communication-service/node-reverse-proxy";

            mockMvc.perform(get(base + "/" + nodeName + "/foo/bar")
                    .header("X-Inbound", "123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Downstream", "yes"))
                .andExpect(content().string("OK"));

            verify(connection).setRequestMethod("GET");
            verify(connection).setRequestProperty("X-Inbound", "123");

            var expected = flowUrl() + ":" + flowPort() + "/graph-x/node-a/foobar";
            org.junit.jupiter.api.Assertions.assertTrue(
                capturedTargetUrl.toString().contains(expected),
                "Expected URL to contain: " + expected + " but was: " + capturedTargetUrl
            );
        }
    }

    @Test
    void proxyToNode_post_streamsRequestBodyToDownstream() throws Exception {
        var nodeName = "nodeB";
        when(nodeProxyManager.getRegisteredEndpointFor(nodeName)).thenReturn("graph-y/node-b");

        var downstreamBody = new ByteArrayOutputStream();

        var connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode()).thenReturn(201);
        when(connection.getHeaderFields()).thenReturn(Map.of());
        when(connection.getOutputStream()).thenReturn(downstreamBody);
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream("CREATED".getBytes()));

        try (var mockedUrl =
                 mockConstruction(URL.class, (mock, context) -> {
                     when(mock.openConnection()).thenReturn(connection);
                 })) {

            var base = "/ubiquia/core/communication-service/node-reverse-proxy";

            mockMvc.perform(post(base + "/" + nodeName + "/some/path")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content("hello downstream"))
                .andExpect(status().isCreated())
                .andExpect(content().string("CREATED"));

            verify(connection).setRequestMethod("POST");
            verify(connection).setDoOutput(true);

            org.junit.jupiter.api.Assertions.assertEquals("hello downstream", downstreamBody.toString());
        }
    }
}
