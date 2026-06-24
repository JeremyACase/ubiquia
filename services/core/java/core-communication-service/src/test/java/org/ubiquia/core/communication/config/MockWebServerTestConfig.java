package org.ubiquia.core.communication.config;

import java.io.IOException;
import okhttp3.mockwebserver.MockWebServer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;
import org.ubiquia.common.library.api.config.BeliefStateGeneratorServiceConfig;

/** Test configuration that wires a MockWebServer in place of real downstream services. */
@TestConfiguration
public class MockWebServerTestConfig {

    /** Creates and starts a MockWebServer bean. */
    @Bean(destroyMethod = "shutdown")
    public MockWebServer mockWebServer() throws IOException {
        var server = new MockWebServer();
        server.start();
        return server;
    }

    /** Provides a WebClient.Builder for test use. */
    @Bean(name = "testWebClientBuilder")
    @Primary
    public WebClient.Builder testWebClientBuilder() {
        return WebClient.builder();
    }

    /** Provides a WebClient built from the test builder. */
    @Bean(name = "testWebClient")
    @Primary
    public WebClient testWebClient(@Qualifier("testWebClientBuilder") WebClient.Builder builder) {
        return builder.build();
    }

    /** Provides a BeliefStateGeneratorServiceConfig pointed at the MockWebServer. */
    @Bean
    @Primary
    public BeliefStateGeneratorServiceConfig beliefStateGeneratorServiceConfig(
        MockWebServer server) {
        return new BeliefStateGeneratorServiceConfig() {
            @Override
            public String getUrl() {
                return "http://" + server.getHostName();
            }

            @Override
            public Integer getPort() {
                return server.getPort();
            }
        };
    }
}
