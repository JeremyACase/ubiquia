package org.ubiquia.core.communication.controller.util.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.IOException;
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
import org.ubiquia.core.communication.config.DashboardServiceConfig;
import org.ubiquia.core.communication.service.io.flow.DeployedGraphPoller;

@WebMvcTest(controllers = DashboardProxyController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.task.scheduling.enabled=false")
class DashboardProxyControllerTest {

    private static final String BASE = "/ubiquia/core/communication-service/dashboard";

    @Autowired
    private MockMvc mockMvc;

    private MockWebServer server;

    @MockitoBean
    private DashboardServiceConfig dashboardServiceConfig;

    @MockitoBean
    private DeployedGraphPoller deployedGraphPoller;

    @BeforeEach
    void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();
        given(this.dashboardServiceConfig.getUrl())
            .willReturn("http://" + this.server.getHostName());
        given(this.dashboardServiceConfig.getPort())
            .willReturn(this.server.getPort());
    }

    @AfterEach
    void tearDown() throws IOException {
        this.server.shutdown();
    }

    @Test
    @DisplayName("HTML response → <base href> injected and root-absolute asset URLs rewritten")
    void htmlResponseInjectsBaseHrefAndRewritesAssetUrls() throws Exception {
        var html = """
            <!doctype html><html><head></head>
            <body>
              <link rel="stylesheet" href="/assets/main.css">
              <script src="/assets/app.js"></script>
              <img src="/logo.png">
            </body></html>
            """;

        this.server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/html; charset=utf-8")
            .setBody(html));

        var result = this.mockMvc.perform(get(BASE + "/"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "text/html; charset=utf-8"))
            .andReturn().getResponse();

        var body = result.getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("<base href=\"" + BASE + "/\">");
        assertThat(body).contains("href=\"" + BASE + "/assets/main.css\"");
        assertThat(body).contains("src=\"" + BASE + "/assets/app.js\"");
        assertThat(body).contains("src=\"" + BASE + "/logo.png\"");
    }

    @Test
    @DisplayName("CSS response → root-absolute url() and @import references rewritten")
    void cssResponseRewritesRootAbsoluteUrls() throws Exception {
        var css = "body { background: url(/images/bg.png); } @import \"/fonts/inter.css\";";

        this.server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/css")
            .setBody(css));

        var result = this.mockMvc.perform(get(BASE + "/assets/main.css"))
            .andExpect(status().isOk())
            .andReturn().getResponse();

        var body = result.getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("url(" + BASE + "/images/bg.png)");
        assertThat(body).contains("@import \"" + BASE + "/fonts/inter.css\"");
    }

    @Test
    @DisplayName("JS asset passed through without modification")
    void jsAssetPassedThroughUnmodified() throws Exception {
        var js = "console.log('ubiquia-dashboard');";

        this.server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/javascript")
            .setBody(js));

        var result = this.mockMvc.perform(get(BASE + "/assets/app.js"))
            .andExpect(status().isOk())
            .andReturn().getResponse();

        assertThat(result.getContentAsString()).isEqualTo(js);
    }

    @Test
    @DisplayName("Static asset returning HTML triggers index-fallback retry from upstream root")
    void staticAssetHtmlFallbackRetriedFromUpstreamRoot() throws Exception {
        // First request: .js path returns HTML (SPA index fallback)
        this.server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/html")
            .setBody("<html><body>index</body></html>"));

        // Second request (retry): upstream root serves the actual asset
        this.server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/javascript")
            .setBody("export default {};"));

        var result = this.mockMvc.perform(get(BASE + "/assets/chunk.js"))
            .andExpect(status().isOk())
            .andReturn().getResponse();

        assertThat(result.getContentAsString()).isEqualTo("export default {};");
        assertThat(this.server.getRequestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("POST forwards body and custom headers; strips hop-by-hop headers")
    void postForwardsBodyAndStripsHopByHopHeaders() throws Exception {
        this.server.enqueue(new MockResponse()
            .setResponseCode(202)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"queued\":true}"));

        this.mockMvc.perform(post(BASE + "/api/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Request-Id", "abc-123")
                // TE and Upgrade are hop-by-hop and must not reach the upstream
                .header("TE", "trailers")
                .header("Upgrade", "websocket")
                .content("{\"data\":\"test\"}"))
            .andExpect(status().isAccepted())
            .andExpect(content().json("{\"queued\":true}"));

        var recorded = this.server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getBody().readUtf8()).isEqualTo("{\"data\":\"test\"}");
        // Custom application headers are forwarded
        assertThat(recorded.getHeader("X-Request-Id")).isEqualTo("abc-123");
        // Accept-Encoding is forced to identity for body rewriting
        assertThat(recorded.getHeader("Accept-Encoding")).isEqualTo("identity");
        // Hop-by-hop headers are stripped before reaching the upstream
        assertThat(recorded.getHeader("TE")).isNull();
        assertThat(recorded.getHeader("Upgrade")).isNull();
    }

    @Test
    @DisplayName("Upstream 404 is forwarded as-is")
    void upstreamErrorStatusForwarded() throws Exception {
        this.server.enqueue(new MockResponse()
            .setResponseCode(404)
            .setHeader("Content-Type", "text/plain")
            .setBody("not found"));

        this.mockMvc.perform(get(BASE + "/missing-page"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Root-absolute Location header is rewritten to proxied prefix")
    void locationHeaderRewrittenToProxiedPrefix() throws Exception {
        this.server.enqueue(new MockResponse()
            .setResponseCode(302)
            .setHeader("Location", "/login")
            .setBody(""));

        this.mockMvc.perform(get(BASE + "/protected"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", BASE + "/login"));
    }

    @Test
    @DisplayName("Query string is forwarded to upstream")
    void queryStringForwardedToUpstream() throws Exception {
        this.server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("[]"));

        this.mockMvc.perform(get(BASE + "/api/items?page=0&size=20"))
            .andExpect(status().isOk());

        var recorded = this.server.takeRequest();
        assertThat(recorded.getRequestUrl()).isNotNull();
        assertThat(recorded.getRequestUrl().queryParameter("page")).isEqualTo("0");
        assertThat(recorded.getRequestUrl().queryParameter("size")).isEqualTo("20");
    }

    @Test
    @DisplayName("X-Ubiquia-Target header set on response")
    void xUbiquiaTargetHeaderPresent() throws Exception {
        this.server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/plain")
            .setBody("ok"));

        this.mockMvc.perform(get(BASE + "/health"))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Ubiquia-Target"));
    }
}
