package org.ubiquia.core.flow.service.manager;

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
import org.ubiquia.common.model.ubiquia.embeddable.EgressSettings;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.enums.AdapterType;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;
import org.ubiquia.common.model.ubiquia.enums.HttpOutputType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;


@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ComponentManagerTest {

    @Autowired
    private ComponentManager componentManager;

    @Autowired
    private GraphController graphController;

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    @Transactional
    public void assertDeploysAgents_throwsIllegalArgumentException()
        throws Exception {

        var graph = this.dummyFactory.generateGraph();

        var ingressAgent = this.dummyFactory.generateComponent();
        ingressAgent.setComponentType(ComponentType.POD);
        var hiddenAgent = this.dummyFactory.generateComponent();
        hiddenAgent.setComponentType(ComponentType.POD);
        graph.getComponents().add(ingressAgent);
        graph.getComponents().add(hiddenAgent);

        var ingressAdapter = this.dummyFactory.generateAdapter();
        ingressAdapter.setAdapterType(AdapterType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressAdapter.getInputSubSchemas().add(subSchema);
        ingressAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));

        var hiddenAdapter = this.dummyFactory.generateAdapter();
        hiddenAdapter.setAdapterType(AdapterType.HIDDEN);
        hiddenAdapter.setEgressSettings(new EgressSettings());
        hiddenAdapter.getEgressSettings().setHttpOutputType(HttpOutputType.PUT);
        hiddenAdapter.setEndpoint("http://localhost:8080/test");
        hiddenAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));

        ingressAgent.setAdapter(ingressAdapter);
        hiddenAgent.setAdapter(hiddenAdapter);

        var edge = new GraphEdge();
        edge.setLeftAdapterName(ingressAdapter.getName());
        edge.setRightAdapterNames(new ArrayList<>());
        edge.getRightAdapterNames().add(hiddenAdapter.getName());
        graph.getEdges().add(edge);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getName());
        deployment.setVersion(graph.getVersion());

        // Should throw exception since K8s is not enabled.
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            this.componentManager.tryDeployComponentsFor(
                deployment);
        });
    }

    @Test
    @Transactional
    public void assertDeploysAgents_isValid() throws Exception {
        var graph = this.dummyFactory.generateGraph();

        var ingressAgent = this.dummyFactory.generateComponent();
        ingressAgent.setComponentType(ComponentType.TEMPLATE);
        var hiddenAgent = this.dummyFactory.generateComponent();
        hiddenAgent.setComponentType(ComponentType.TEMPLATE);
        graph.getComponents().add(ingressAgent);
        graph.getComponents().add(hiddenAgent);

        var ingressAdapter = this.dummyFactory.generateAdapter();
        ingressAdapter.setAdapterType(AdapterType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressAdapter.getInputSubSchemas().add(subSchema);
        ingressAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));

        var hiddenAdapter = this.dummyFactory.generateAdapter();
        hiddenAdapter.setAdapterType(AdapterType.HIDDEN);
        hiddenAdapter.setEgressSettings(new EgressSettings());
        hiddenAdapter.getEgressSettings().setHttpOutputType(HttpOutputType.PUT);
        hiddenAdapter.setEndpoint("http://localhost:8080/test");
        hiddenAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));

        ingressAgent.setAdapter(ingressAdapter);
        hiddenAgent.setAdapter(hiddenAdapter);

        var edge = new GraphEdge();
        edge.setLeftAdapterName(ingressAdapter.getName());
        edge.setRightAdapterNames(new ArrayList<>());
        edge.getRightAdapterNames().add(hiddenAdapter.getName());
        graph.getEdges().add(edge);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getName());
        deployment.setVersion(graph.getVersion());

        // Should throw exception since K8s is not enabled.
        Assertions.assertDoesNotThrow(() -> {
            this.componentManager.tryDeployComponentsFor(
                deployment);
        });
    }
}