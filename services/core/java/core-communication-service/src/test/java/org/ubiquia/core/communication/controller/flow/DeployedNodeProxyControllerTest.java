package org.ubiquia.core.communication.controller.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.IOException;
import java.util.List;
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
import org.ubiquia.core.communication.service.manager.flow.NodeProxyManager;

@WebMvcTest(controllers = DeployedNodeProxyController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.task.scheduling.enabled=false")
class DeployedNodeProxyControllerTest {

    private static final String GRAPH = "my-graph";
    private static final String BASE = "/ubiquia/core-communication-service";

    @Autowired
    private MockMvc mockMvc;

    private MockWebServer server;

    @MockitoBean
    private NodeProxyManager nodeProxyManager;

    @MockitoBean
    private FlowServiceConfig flowServiceConfig;

    @MockitoBean
    private DeployedGraphPoller deployedGraphPoller;

    @BeforeEach
    void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();
        given(this.flowServiceConfig.getBaseUrl())
            .willReturn("http://" + this.server.getHostName() + ":" + this.server.getPort());
    }

    @AfterEach
    void tearDown() throws IOException {
        this.server.shutdown();
    }

    @Test
    @DisplayName("GET /node/get-proxied-urls returns registered endpoints")
    void getProxiedUrls_returnsRegisteredEndpoints() throws Exception {
        given(this.nodeProxyManager.getRegisteredEndpoints()).willReturn(List.of("a/b", "c/d"));

        this.mockMvc.perform(get(BASE + "/node/get-proxied-urls"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json("[\"a/b\",\"c/d\"]"));
    }

    @Test
    @DisplayName("Unknown node → 400 BAD_REQUEST")
    void unknownNode_returns400() throws Exception {
        given(this.nodeProxyManager.getRegisteredEndpointForNodeName("missing")).willReturn(null);

        this.mockMvc.perform(get(BASE + "/" + GRAPH + "/node/missing/foo"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET forwards status, headers, and body to downstream")
    void get_forwardsResponseToClient() throws Exception {
        given(this.nodeProxyManager.getRegisteredEndpointForNodeName("nodeA"))
            .willReturn("registered");

        this.server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/plain")
            .setHeader("X-Downstream", "yes")
            .setBody("OK"));

        this.mockMvc.perform(get(BASE + "/" + GRAPH + "/node/nodeA/foo/bar")
                .header("X-Inbound", "123"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Downstream", "yes"))
            .andExpect(content().string("OK"));

        var recorded = this.server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getHeader("X-Inbound")).isEqualTo("123");
        assertThat(recorded.getPath())
            .isEqualTo("/ubiquia/core-flow-service/" + GRAPH + "/node/nodea/foo/bar");
    }

    @Test
    @DisplayName("POST streams request body to downstream")
    void post_streamsBodyToDownstream() throws Exception {
        given(this.nodeProxyManager.getRegisteredEndpointForNodeName("nodeB"))
            .willReturn("registered");

        this.server.enqueue(new MockResponse()
            .setResponseCode(201)
            .setBody("CREATED"));

        this.mockMvc.perform(post(BASE + "/" + GRAPH + "/node/nodeB/some/path")
                .contentType(MediaType.TEXT_PLAIN)
                .content("hello downstream"))
            .andExpect(status().isCreated())
            .andExpect(content().string("CREATED"));

        var recorded = this.server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getBody().readUtf8()).isEqualTo("hello downstream");
    }

    @Test
    @DisplayName("HTML response is passed through without rewriting (rewritesHtmlAndCss=false)")
    void htmlResponse_notRewritten() throws Exception {
        given(this.nodeProxyManager.getRegisteredEndpointForNodeName("nodeC"))
            .willReturn("registered");

        var html = "<html><body><a href=\"/some/link\">link</a></body></html>";
        this.server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/html")
            .setBody(html));

        var result = this.mockMvc.perform(get(BASE + "/" + GRAPH + "/node/nodeC/page"))
            .andExpect(status().isOk())
            .andReturn().getResponse();

        assertThat(result.getContentAsString()).doesNotContain("<base href");
        assertThat(result.getContentAsString()).contains("/some/link");
    }
}
