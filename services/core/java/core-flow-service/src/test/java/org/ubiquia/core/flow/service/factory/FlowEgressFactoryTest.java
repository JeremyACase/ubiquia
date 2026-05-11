package org.ubiquia.core.flow.service.factory;

import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.core.flow.TestHelper;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FlowEgressFactoryTest {

    @Autowired
    private FlowEgressFactory flowEgressFactory;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
        this.flowEgressFactory.teardown();
    }

    @AfterEach
    public void cleanup() {
        this.flowEgressFactory.teardown();
    }

    @Test
    public void assertTryBuildEgressFor_withEmptyPeers_doesNotCreateRelay() {
        this.flowEgressFactory.tryBuildEgressFor(Set.of());

        var relay = ReflectionTestUtils.getField(this.flowEgressFactory, "relay");
        Assertions.assertNull(relay);
    }

    @Test
    public void assertTryBuildEgressFor_withPeers_createsRelay() {
        this.flowEgressFactory.tryBuildEgressFor(Set.of("http://peer-a:8080"));

        var relay = ReflectionTestUtils.getField(this.flowEgressFactory, "relay");
        Assertions.assertNotNull(relay);
    }

    @Test
    public void assertTryBuildEgressFor_calledTwice_reusesSameRelayInstance() {
        this.flowEgressFactory.tryBuildEgressFor(Set.of("http://peer-a:8080"));
        var firstRelay = ReflectionTestUtils.getField(this.flowEgressFactory, "relay");

        this.flowEgressFactory.tryBuildEgressFor(Set.of("http://peer-b:8080"));
        var secondRelay = ReflectionTestUtils.getField(this.flowEgressFactory, "relay");

        Assertions.assertSame(firstRelay, secondRelay);
    }

    @Test
    public void assertTeardown_withActiveRelay_clearsRelayField() {
        this.flowEgressFactory.tryBuildEgressFor(Set.of("http://peer-a:8080"));
        this.flowEgressFactory.teardown();

        var relay = ReflectionTestUtils.getField(this.flowEgressFactory, "relay");
        Assertions.assertNull(relay);
    }

    @Test
    public void assertTryBuildEgressFor_afterTeardown_createsNewRelayInstance() {
        this.flowEgressFactory.tryBuildEgressFor(Set.of("http://peer-a:8080"));
        var firstRelay = ReflectionTestUtils.getField(this.flowEgressFactory, "relay");
        this.flowEgressFactory.teardown();

        this.flowEgressFactory.tryBuildEgressFor(Set.of("http://peer-a:8080"));
        var secondRelay = ReflectionTestUtils.getField(this.flowEgressFactory, "relay");

        Assertions.assertNotNull(secondRelay);
        Assertions.assertNotSame(firstRelay, secondRelay);
    }
}
