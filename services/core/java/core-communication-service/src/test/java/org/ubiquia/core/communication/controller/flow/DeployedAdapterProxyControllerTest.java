package org.ubiquia.core.communication.controller.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.core.communication.config.MockWebServerTestConfig;
import org.ubiquia.core.communication.service.io.flow.DeployedGraphPoller;
import org.ubiquia.core.communication.service.manager.flow.AdapterProxyManager;

@WebMvcTest(controllers = DeployedAdapterProxyController.class)
@Import({MockWebServerTestConfig.class})
class DeployedAdapterProxyControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MockWebServer server;

    private String baseUrl;

    @MockitoBean
    private FlowServiceConfig flowServiceConfig;

    @MockitoBean
    private AdapterProxyManager adapterProxyManager;

    // Prevent background polling from interfering with the slice
    @MockitoBean
    private DeployedGraphPoller deployedGraphPoller;

    @BeforeEach
    void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();
        this.baseUrl = "http://" + this.server.getHostName();
        given(this.flowServiceConfig.getUrl()).willReturn(this.baseUrl);
        given(this.flowServiceConfig.getPort()).willReturn(this.server.getPort());
    }

    @Test
    void getProxiedUrls_returnsList() throws Exception {
        when(adapterProxyManager.getRegisteredEndpoints())
            .thenReturn(List.of("adapter-a", "adapter-b"));

        mockMvc.perform(get("/ubiquia/communication-service/adapter-reverse-proxy/get-proxied-urls"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0]").value("adapter-a"))
            .andExpect(jsonPath("$[1]").value("adapter-b"));
    }

    @Test
    void proxy_get_forwardsToRegisteredEndpoint_andReturnsBodyAndHeaders() throws Exception {
        final String adapterName = "my-adapter";
        final String registeredEndpoint = "registered/endpoint";
        when(adapterProxyManager.getRegisteredEndpointFor(adapterName)).thenReturn(registeredEndpoint);

        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("X-Downstream", "yes")
            .setBody("pong"));

        mockMvc.perform(get("/ubiquia/communication-service/adapter-reverse-proxy/{adapterName}/ping", adapterName)
                .header("X-Client", "hello"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Downstream", "yes"))
            .andExpect(content().string("pong"));

        // --- Timeout to avoid indefinite hang if the proxy never calls downstream
        RecordedRequest rr = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(rr).as("proxy never hit MockWebServer").isNotNull();
        assertThat(rr.getMethod()).isEqualTo("GET");
        // cleanedPath for “…/{adapterName}/ping” becomes “ping”; final path: “/{registeredEndpoint}/ping”
        assertThat(rr.getPath()).isEqualTo("/" + registeredEndpoint + "/ping");
        assertThat(rr.getHeader("X-Client")).isEqualTo("hello");
    }

    @Test
    void proxy_post_forwardsBody() throws Exception {
        final String adapterName = "schema";
        final String registeredEndpoint = "lint/json";
        when(adapterProxyManager.getRegisteredEndpointFor(adapterName)).thenReturn(registeredEndpoint);

        server.enqueue(new MockResponse()
            .setResponseCode(201)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"ok\":true}"));

        String requestJson = "{\"hello\":\"world\"}";

        mockMvc.perform(post("/ubiquia/communication-service/adapter-reverse-proxy/{adapterName}/validate", adapterName)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json("{\"ok\":true}"));

        RecordedRequest rr = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(rr).as("proxy never hit MockWebServer").isNotNull();
        assertThat(rr.getMethod()).isEqualTo("POST");
        assertThat(rr.getPath()).isEqualTo("/" + registeredEndpoint + "/validate");
        assertThat(rr.getBody().readString(StandardCharsets.UTF_8)).isEqualTo(requestJson);
    }

    @Test
    void proxy_put_forwardsBody() throws Exception {
        final String adapterName = "files";
        final String registeredEndpoint = "upload";
        when(adapterProxyManager.getRegisteredEndpointFor(adapterName)).thenReturn(registeredEndpoint);

        server.enqueue(new MockResponse().setResponseCode(204));

        var payload = "BYTES".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(put("/ubiquia/communication-service/adapter-reverse-proxy/{adapterName}/chunk", adapterName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(payload))
            .andExpect(status().isNoContent());

        RecordedRequest rr = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(rr).as("proxy never hit MockWebServer").isNotNull();
        assertThat(rr.getMethod()).isEqualTo("PUT");
        assertThat(rr.getPath()).isEqualTo("/" + registeredEndpoint + "/chunk");
        assertThat(rr.getBody().readByteArray()).isEqualTo(payload);
    }

    @Test
    void proxy_whenAdapterNotRegistered_returnsServerError() throws Exception {
        final String adapterName = "unknown";
        when(adapterProxyManager.getRegisteredEndpointFor(adapterName)).thenReturn(null);

        mockMvc.perform(get("/ubiquia/communication-service/adapter-reverse-proxy/{adapterName}/anything", adapterName))
            .andExpect(status().is4xxClientError());
    }
}
