package org.ubiquia.core.flow.service.logic.adapter;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.ubiquia.common.model.ubiquia.dto.GraphEdge;
import org.ubiquia.common.model.ubiquia.embeddable.EgressSettings;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.enums.AdapterType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.component.adapter.EgressAdapter;
import org.ubiquia.core.flow.component.adapter.HiddenAdapter;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.mock.MockRegistrar;
import org.ubiquia.core.flow.model.adapter.AdapterContext;
import org.ubiquia.core.flow.repository.AdapterRepository;
import org.ubiquia.core.flow.service.manager.AdapterManager;


@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AdapterInboxPollingLogicTest {

    @Autowired
    private AdapterInboxPollingLogic adapterInboxPollingLogic;

    @Autowired
    private AdapterManager adapterManager;

    @Autowired
    private AdapterRepository adapterRepository;

    @Autowired
    private GraphController graphController;

    @Autowired
    private MockRegistrar mockRegistrar;

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    public void assertTemplateAdapterIsValidToPollInbox_isValid() {
        var adapter = new HiddenAdapter();
        var adapterContext = new AdapterContext();
        adapter.setAdapterContext(adapterContext);
        adapterContext.setAdapterType(AdapterType.HIDDEN);
        adapterContext.setTemplateComponent(true);

        var valid = this.adapterInboxPollingLogic.isValidToPollInbox(adapter);
        Assertions.assertTrue(valid);
    }

    @Test
    public void assertAdapterWithoutAgentIsValidToPollInbox_isValid() {
        var adapter = new EgressAdapter();
        var adapterContext = new AdapterContext();
        adapter.setAdapterContext(adapterContext);
        adapterContext.setEgressSettings(new EgressSettings());
        adapterContext.setAdapterType(AdapterType.EGRESS);
        adapterContext.setTemplateComponent(false);

        var valid = this.adapterInboxPollingLogic.isValidToPollInbox(adapter);
        Assertions.assertTrue(valid);
    }

    @Test
    public void assertAdapterWithoutAgentIsNotValidToPollInbox_isValid() {
        var adapter = new EgressAdapter();
        var adapterContext = new AdapterContext();
        adapter.setAdapterContext(adapterContext);
        adapterContext.setEgressSettings(new EgressSettings());
        adapterContext.setAdapterType(AdapterType.EGRESS);
        adapterContext.setTemplateComponent(false);
        ReflectionTestUtils.setField(adapterContext, "openMessages", 100);

        var valid = this.adapterInboxPollingLogic.isValidToPollInbox(adapter);
        Assertions.assertFalse(valid);
    }

    @Test
    public void assertHiddenAdapterValidToPollInbox_isValid() throws Exception {

        var graph = this.dummyFactory.generateGraph();

        var ingressComponent = this.dummyFactory.generateComponent();
        var hiddenComponent = this.dummyFactory.generateComponent();
        graph.getComponents().add(ingressComponent);
        graph.getComponents().add(hiddenComponent);

        var ingressAdapter = this.dummyFactory.generateAdapter();
        ingressAdapter.setAdapterType(AdapterType.PUSH);

        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressAdapter.getInputSubSchemas().add(subSchema);
        ingressAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        ingressComponent.setAdapter(ingressAdapter);

        var hiddenAdapter = this.dummyFactory.generateAdapter();
        hiddenAdapter.setAdapterType(AdapterType.HIDDEN);
        hiddenAdapter.setEndpoint("http://localhost:8080/test");
        hiddenAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
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

        var adapter = this.testHelper.findAdapter(
            hiddenAdapter.getName(),
            graph.getName());

        adapter.getAdapterContext().setTemplateComponent(false);

        var valid = this.adapterInboxPollingLogic.isValidToPollInbox(adapter);
        Assertions.assertTrue(valid);
    }

    @Test
    public void assertHiddenAdapterIsNotValidToPollInbox_isValid() throws Exception {

        var graph = this.dummyFactory.generateGraph();

        var ingressComponent = this.dummyFactory.generateComponent();
        var hiddenComponent = this.dummyFactory.generateComponent();
        graph.getComponents().add(ingressComponent);
        graph.getComponents().add(hiddenComponent);

        var ingressAdapter = this.dummyFactory.generateAdapter();
        ingressAdapter.setAdapterType(AdapterType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressAdapter.getInputSubSchemas().add(subSchema);
        ingressAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        ingressComponent.setAdapter(ingressAdapter);

        var hiddenAdapter = this.dummyFactory.generateAdapter();
        hiddenAdapter.setAdapterType(AdapterType.HIDDEN);
        hiddenAdapter.setEndpoint("http://localhost:8080/test");
        hiddenAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
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

        var adapter = this.testHelper.findAdapter(
            hiddenAdapter.getName(),
            graph.getName());

        var adapterContext = adapter.getAdapterContext();
        adapterContext.setTemplateComponent(false);
        ReflectionTestUtils.setField(adapterContext, "openMessages", 100);

        var valid = this.adapterInboxPollingLogic.isValidToPollInbox(adapter);
        Assertions.assertFalse(valid);
    }
}
