package org.ubiquia.core.communication.controller.belief;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.ubiquia.common.model.ubiquia.embeddable.BeliefStateGeneration;
import org.ubiquia.core.communication.config.MockWebServerTestConfig;
import reactor.test.StepVerifier;

@WebFluxTest(controllers = BeliefStateGeneratorControllerProxy.class)
@Import(MockWebServerTestConfig.class)
class BeliefStateGeneratorControllerProxyTest {

    @Autowired
    private BeliefStateGeneratorControllerProxy controller;
    @Autowired
    private MockWebServer server;

    @BeforeEach
    void beforeEach() throws InterruptedException {
        drainRequests();
    }

    @Test
    void getUrlHelper_buildsExpectedBase() {
        var url = this.controller.getUrlHelper();
        assertThat(url).startsWith("http://");
        assertThat(url).endsWith("/belief-state-generator");
    }

    @Test
    void proxyGenerate_happyPath_forwardsAndReturnsResponse_andBuildsCorrectUriAndBody() throws Exception {
        this.server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .setBody("{}"));

        var requestBody = new BeliefStateGeneration();

        var result = this.controller.proxyGenerate(requestBody);

        StepVerifier.create(result)
            .assertNext(re -> {
                assertThat(re.getStatusCode().is2xxSuccessful()).isTrue();
                assertThat(re.getBody()).isNotNull();
            })
            .verifyComplete();

        var rr = takeLastRequest();
        assertThat(rr).isNotNull();
        assertThat(rr.getMethod()).isEqualTo("POST");
        assertThat(rr.getPath()).isEqualTo("/belief-state-generator/belief-state/generate");
        assertThat(rr.getHeader("Content-Type")).contains("application/json");
        assertThat(rr.getBody().readUtf8()).isNotBlank();
    }

    @Test
    void proxyTeardown_happyPath_forwardsAndReturnsResponse_andBuildsCorrectUriAndBody() throws Exception {
        this.server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .setBody("{}"));

        var requestBody = new BeliefStateGeneration();

        var result = this.controller.proxyTeardown(requestBody);

        StepVerifier.create(result)
            .assertNext(re -> {
                assertThat(re.getStatusCode().is2xxSuccessful()).isTrue();
                assertThat(re.getBody()).isNotNull();
            })
            .verifyComplete();

        var rr = takeLastRequest();
        assertThat(rr).isNotNull();
        assertThat(rr.getMethod()).isEqualTo("POST");
        assertThat(rr.getPath()).isEqualTo("/belief-state-generator/belief-state/teardown");
        assertThat(rr.getHeader("Content-Type")).contains("application/json");
        assertThat(rr.getBody().readUtf8()).isNotBlank();
    }

    @Test
    void proxyGenerate_propagatesErrorSignal_on5xx() {
        this.server.enqueue(new MockResponse().setResponseCode(500));

        var requestBody = new BeliefStateGeneration();

        StepVerifier.create(this.controller.proxyGenerate(requestBody))
            .expectError()
            .verify();
    }

    @Test
    void proxyTeardown_propagatesErrorSignal_on4xx() {
        this.server.enqueue(new MockResponse().setResponseCode(404));

        var requestBody = new BeliefStateGeneration();

        StepVerifier.create(this.controller.proxyTeardown(requestBody))
            .expectError()
            .verify();
    }

    private void drainRequests() throws InterruptedException {

        // Drain any leftover requests from prior tests
        while (Objects.nonNull(this.server.takeRequest(
            25,
            TimeUnit.MILLISECONDS))) {
        }
    }

    private RecordedRequest takeLastRequest() throws InterruptedException {

        var last = this.server.takeRequest(1, TimeUnit.SECONDS); // wait for first

        if (Objects.nonNull(last)) {
            RecordedRequest next;
            while (Objects.nonNull(
                next = this.server.takeRequest(100, TimeUnit.MILLISECONDS))) {
                last = next;
            }
        }

        return last;
    }
}
