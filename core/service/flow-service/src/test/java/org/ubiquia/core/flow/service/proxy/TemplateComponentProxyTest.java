package org.ubiquia.core.flow.service.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.ubiquia.common.model.ubiquia.dto.GraphEdge;
import org.ubiquia.common.model.ubiquia.embeddable.AdapterSettings;
import org.ubiquia.common.model.ubiquia.embeddable.EgressSettings;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.enums.AdapterType;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;
import org.ubiquia.common.model.ubiquia.enums.HttpOutputType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.component.adapter.HiddenAdapter;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.service.builder.FlowEventBuilder;
import org.ubiquia.core.flow.service.visitor.validator.PayloadModelValidator;


@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TemplateComponentProxyTest {

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private FlowEventBuilder flowEventBuilder;

    @Autowired
    private GraphController graphController;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PayloadModelValidator payloadModelValidator;

    @Autowired
    private TemplateComponentProxy templateComponentProxy;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    @Transactional
    public void assertGeneratesFuzzyData_isValid() throws Exception {

        var graph = this.dummyFactory.generateGraph();

        var ingressComponent = this.dummyFactory.generateComponent();
        var hiddenComponent = this.dummyFactory.generateComponent();
        hiddenComponent.setComponentType(ComponentType.NONE);
        graph.getComponents().add(ingressComponent);
        graph.getComponents().add(hiddenComponent);

        var ingressAdapter = this.dummyFactory.generateAdapter();
        ingressAdapter.setAdapterType(AdapterType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressAdapter.getInputSubSchemas().add(subSchema);
        ingressAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));

        var hiddenAdapter = this.dummyFactory.generateAdapter();
        hiddenAdapter.setAdapterType(AdapterType.HIDDEN);
        hiddenAdapter.setEgressSettings(new EgressSettings());
        hiddenAdapter.getEgressSettings().setHttpOutputType(HttpOutputType.PUT);
        hiddenAdapter.setAdapterSettings(new AdapterSettings());
        hiddenAdapter.getAdapterSettings().setValidateInputPayload(true);
        hiddenAdapter.getAdapterSettings().setPersistOutputPayload(true);
        hiddenAdapter.setEndpoint("http://localhost:8080/test");
        hiddenAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        hiddenAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Cat"));

        ingressComponent.setAdapter(ingressAdapter);
        hiddenComponent.setAdapter(hiddenAdapter);

        var edge = new GraphEdge();
        edge.setLeftAdapterName(ingressAdapter.getName());
        edge.setRightAdapterNames(new ArrayList<>());
        edge.getRightAdapterNames().add(hiddenAdapter.getName());
        graph.getEdges().add(edge);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getName());
        deployment.setVersion(graph.getVersion());
        this.graphController.tryDeployGraph(deployment);

        var adapter = (HiddenAdapter) this
            .testHelper
            .findAdapter(hiddenAdapter.getName(), graph.getName());

        var flowEvent = this.flowEventBuilder.makeEventFrom(
            "test",
            adapter);

        this.templateComponentProxy.proxyAsComponentFor(flowEvent);
        var fuzzyData = this.objectMapper.readValue(
            flowEvent.getOutputPayload(),
            Object.class);

        Assertions.assertDoesNotThrow(() -> {
            this.payloadModelValidator.tryValidateOutputPayloadFor(
                this.objectMapper.writeValueAsString(fuzzyData),
                adapter);
        });
    }
}