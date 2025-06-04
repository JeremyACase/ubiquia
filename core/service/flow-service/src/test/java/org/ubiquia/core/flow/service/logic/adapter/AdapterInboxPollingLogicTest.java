package org.ubiquia.core.flow.service.logic.adapter;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.ubiquia.common.model.ubiquia.dto.GraphEdgeDto;
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
        adapterContext.setTemplateAgent(true);

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
        adapterContext.setTemplateAgent(false);

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
        adapterContext.setTemplateAgent(false);
        ReflectionTestUtils.setField(adapterContext, "openMessages", 100);

        var valid = this.adapterInboxPollingLogic.isValidToPollInbox(adapter);
        Assertions.assertFalse(valid);
    }

    @Test
    public void assertHiddenAdapterValidToPollInbox_isValid() throws Exception {

        var graph = this.dummyFactory.generateGraph();

        var ingressAgent = this.dummyFactory.generateAgent();
        var hiddenAgent = this.dummyFactory.generateAgent();
        graph.getAgents().add(ingressAgent);
        graph.getAgents().add(hiddenAgent);

        var ingressAdapter = this.dummyFactory.generateAdapter();
        ingressAdapter.setAdapterType(AdapterType.PUSH);

        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressAdapter.getInputSubSchemas().add(subSchema);
        ingressAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        ingressAgent.setAdapter(ingressAdapter);

        var hiddenAdapter = this.dummyFactory.generateAdapter();
        hiddenAdapter.setAdapterType(AdapterType.HIDDEN);
        hiddenAdapter.setEndpoint("http://localhost:8080/test");
        hiddenAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        hiddenAgent.setAdapter(hiddenAdapter);

        var edge = new GraphEdgeDto();
        edge.setLeftAdapterName(ingressAdapter.getAdapterName());
        edge.setRightAdapterNames(new ArrayList<>());
        edge.getRightAdapterNames().add(hiddenAdapter.getAdapterName());
        graph.getEdges().add(edge);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getGraphName());
        deployment.setVersion(graph.getVersion());
        this.graphController.tryDeployGraph(deployment);

        var adapter = this.testHelper.findAdapter(
            hiddenAdapter.getAdapterName(),
            graph.getGraphName());

        adapter.getAdapterContext().setTemplateAgent(false);

        var valid = this.adapterInboxPollingLogic.isValidToPollInbox(adapter);
        Assertions.assertTrue(valid);
    }

    @Test
    public void assertHiddenAdapterIsNotValidToPollInbox_isValid() throws Exception {

        var graph = this.dummyFactory.generateGraph();

        var ingressAgent = this.dummyFactory.generateAgent();
        var hiddenAgent = this.dummyFactory.generateAgent();
        graph.getAgents().add(ingressAgent);
        graph.getAgents().add(hiddenAgent);

        var ingressAdapter = this.dummyFactory.generateAdapter();
        ingressAdapter.setAdapterType(AdapterType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressAdapter.getInputSubSchemas().add(subSchema);
        ingressAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        ingressAgent.setAdapter(ingressAdapter);

        var hiddenAdapter = this.dummyFactory.generateAdapter();
        hiddenAdapter.setAdapterType(AdapterType.HIDDEN);
        hiddenAdapter.setEndpoint("http://localhost:8080/test");
        hiddenAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        hiddenAgent.setAdapter(hiddenAdapter);

        var edge = new GraphEdgeDto();
        edge.setLeftAdapterName(ingressAdapter.getAdapterName());
        edge.setRightAdapterNames(new ArrayList<>());
        edge.getRightAdapterNames().add(hiddenAdapter.getAdapterName());
        graph.getEdges().add(edge);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getGraphName());
        deployment.setVersion(graph.getVersion());
        this.graphController.tryDeployGraph(deployment);

        var adapter = this.testHelper.findAdapter(
            hiddenAdapter.getAdapterName(),
            graph.getGraphName());

        var adapterContext = adapter.getAdapterContext();
        adapterContext.setTemplateAgent(false);
        ReflectionTestUtils.setField(adapterContext, "openMessages", 100);

        var valid = this.adapterInboxPollingLogic.isValidToPollInbox(adapter);
        Assertions.assertFalse(valid);
    }
}
