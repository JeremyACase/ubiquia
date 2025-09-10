package org.ubiquia.core.communication.controller.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.core.communication.service.io.flow.DeployedGraphPoller;
import org.ubiquia.core.communication.service.manager.flow.ComponentProxyManager;

@WebMvcTest(controllers = ComponentReverseProxyController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.task.scheduling.enabled=false")
class ComponentReverseProxyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private MockWebServer server;
    private String baseUrl;

    @MockitoBean
    private ComponentProxyManager componentProxyManager;

    @MockitoBean
    private FlowServiceConfig flowServiceConfig;

    // Prevent polling
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

    @AfterEach
    void tearDown() throws IOException {
        this.server.shutdown();
    }

    @Test
    @DisplayName("Unknown component → 400 BAD_REQUEST")
    void unknownComponentReturns400() throws Exception {
        given(this.componentProxyManager.getRegisteredEndpointFor("missing")).willReturn(null);

        this.mockMvc.perform(get("/ubiquia/communication-service/component-reverse-proxy/missing/whatever"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Absolute upstream (HTML) → base href injected and URLs rewritten")
    void absoluteUpstreamHtmlRewritten() throws Exception {
        var html = """
            <!doctype html><html><head><link rel="stylesheet" href="/styles.css"></head>
            <body><img src="/logo.png"><script src="app.js"></script></body></html>
            """;

        this.server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/html; charset=iso-8859-1")
            .setBody(html));

        var absoluteBase = "http://"
            + this.server.getHostName()
            + ":"
            + this.server.getPort()
            + "/ui/";

        given(this.componentProxyManager.getRegisteredEndpointFor("workbench"))
            .willReturn(URI.create(absoluteBase));

        mockMvc.perform(get("/ubiquia/communication-service/component-reverse-proxy/workbench/index.html"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Ubiquia-Target", absoluteBase + "index.html"))
            .andExpect(header().string("Content-Type", "text/html; charset=utf-8"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "<base href=\"/ubiquia/communication-service/component-reverse-proxy/workbench/\">"
            )));

        var recorded = server.takeRequest();
        assertThat(recorded.getPath()).isEqualTo("/ui/index.html");
    }

    @Test
    @DisplayName("Static asset returns HTML → retried via host root")
    void staticAssetRetryFromRoot() throws Exception {

        this.server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/html")
            .setBody("<html><body>index</body></html>"));

        this.server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/javascript")
            .setBody("console.log('ok');"));

        var absoluteBase = "http://"
            + this.server.getHostName()
            + ":"
            + this.server.getPort()
            + "/ui/";

        given(this.componentProxyManager.getRegisteredEndpointFor("workbench"))
            .willReturn(URI.create(absoluteBase));

        var res = mockMvc.perform(get("/ubiquia/communication-service/component-reverse-proxy/workbench/assets/app.js"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

        assertThat(res.getContentAsString(StandardCharsets.UTF_8)).contains("console.log('ok');");
        assertThat(this.server.getRequestCount()).isEqualTo(2);
        assertThat(this.server.takeRequest().getPath()).isEqualTo("/assets/app.js");
        assertThat(this.server.takeRequest().getPath()).isEqualTo("/assets/app.js");
    }

    @Test
    @DisplayName("Relative endpoint builds from FlowServiceConfig (url + port)")
    void relativeEndpointUsesFlowServiceConfig() throws Exception {
        this.server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"ok\":true}"));

        given(this.componentProxyManager.getRegisteredEndpointFor("flow-ui"))
            .willReturn(URI.create("/ui/"));

        this.mockMvc.perform(get("/ubiquia/communication-service/component-reverse-proxy/flow-ui/api/ping?x=1"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"ok\":true}"));
    }

    @Test
    @DisplayName("Location header with root-absolute is rewritten to proxied prefix")
    void locationHeaderRewritten() throws Exception {
        this.server.enqueue(new MockResponse()
            .setResponseCode(302)
            .setHeader("Location", "/login")
            .setBody(""));

        given(this.componentProxyManager.getRegisteredEndpointFor("workbench"))
            .willReturn(URI.create("/ui/"));

        this.mockMvc.perform(get("/ubiquia/communication-service/component-reverse-proxy/workbench/secret"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location",
                "/ubiquia/communication-service/component-reverse-proxy/workbench/login"));
    }

    @Test
    @DisplayName("POST forwards body and non hop-by-hop headers")
    void postForwardsBodyAndHeaders() throws Exception {
        this.server.enqueue(new MockResponse()
            .setResponseCode(201)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"id\":123}"));

        given(this.componentProxyManager.getRegisteredEndpointFor("flow-api"))
            .willReturn(URI.create("/api/"));

        this.mockMvc.perform(post("/ubiquia/communication-service/component-reverse-proxy/flow-api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Custom", "abc")
                .content("{\"name\":\"test\"}"))
            .andExpect(status().isCreated())
            .andExpect(content().json("{\"id\":123}"));

        var recorded = this.server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getHeader("X-Custom")).isEqualTo("abc");
        assertThat(recorded.getHeader("Content-Length")).isNotNull();
        assertThat(recorded.getHeader("Accept-Encoding")).isEqualTo("identity");
        assertThat(recorded.getBody().readUtf8()).isEqualTo("{\"name\":\"test\"}");
    }

    @Test
    @DisplayName("GET /get-proxied-urls returns list from manager")
    void getProxiedUrls() throws Exception {
        given(this.componentProxyManager.getRegisteredEndpoints())
            .willReturn(java.util.List.of("/ui/", "/api/"));

        this.mockMvc.perform(get("/ubiquia/communication-service/component-reverse-proxy/get-proxied-urls"))
            .andExpect(status().isOk())
            .andExpect(content().json("[\"/ui/\",\"/api/\"]"));
    }
}
