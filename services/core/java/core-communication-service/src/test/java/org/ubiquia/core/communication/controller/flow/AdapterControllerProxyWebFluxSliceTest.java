package org.ubiquia.core.communication.controller.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetAddress;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.core.communication.config.MockWebServerTestConfig;
import org.ubiquia.core.communication.service.io.flow.DeployedGraphPoller;

@WebMvcTest(
    controllers = AdapterControllerProxy.class,
    properties = "spring.task.scheduling.enabled=false"
)
@Import(MockWebServerTestConfig.class) // provides WebClient bean used by the controller
class AdapterControllerProxyWebMvcSliceTest {

    @Autowired
    private MockMvc mockMvc;

    // Prevent background polling from interfering with the slice
    @MockitoBean
    private DeployedGraphPoller deployedGraphPoller;
    @MockitoBean
    private FlowServiceConfig flowServiceConfig;

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start(InetAddress.getByName("127.0.0.1"), 0);

        Mockito.when(flowServiceConfig.getUrl()).thenReturn("http://" + server.getHostName());
        Mockito.when(flowServiceConfig.getPort()).thenReturn(server.getPort());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void get_queryParams_route_forwards_to_downstream_and_returns_200() throws Exception {
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("null")); // controller maps null -> empty body on the way back

        mockMvc.perform(get("/ubiquia/communication-service/flow-service/adapter/query/params")
                .queryParam("page", "0")
                .queryParam("size", "5")
                .queryParam("sort-descending", "true")
                .queryParam("foo", "bar")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        var recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath())
            .startsWith("/ubiquia/flow-service/adapter/query/params?")
            .contains("page=0")
            .contains("size=5")
            .contains("sort-descending=true")
            .contains("foo=bar");
    }
}
