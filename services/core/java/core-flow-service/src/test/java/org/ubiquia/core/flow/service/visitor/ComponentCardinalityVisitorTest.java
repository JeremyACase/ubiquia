package org.ubiquia.core.flow.service.visitor;

import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.model.ubiquia.dto.GraphEdge;
import org.ubiquia.common.model.ubiquia.embeddable.Cardinality;
import org.ubiquia.common.model.ubiquia.embeddable.CardinalitySetting;
import org.ubiquia.common.model.ubiquia.embeddable.EgressSettings;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.enums.AdapterType;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;
import org.ubiquia.common.model.ubiquia.enums.HttpOutputType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;


@SpringBootTest
public class ComponentCardinalityVisitorTest {

    @Autowired
    private GraphController graphController;

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    public void assertDoesNotDeployAdapter_isValid() throws Exception {

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
        hiddenAdapter.getEgressSettings().setHttpOutputType(HttpOutputType.POST);
        hiddenAdapter.setEndpoint("http://localhost:8080/test");
        hiddenAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));

        ingressComponent.setAdapter(ingressAdapter);
        hiddenComponent.setAdapter(hiddenAdapter);

        var edge = new GraphEdge();
        edge.setLeftAdapterName(ingressAdapter.getName());
        edge.setRightAdapterNames(new ArrayList<>());
        edge.getRightAdapterNames().add(hiddenAdapter.getName());
        graph.getEdges().add(edge);

        this.graphController.register(graph);

        var setting = new CardinalitySetting();
        setting.setEnabled(false);
        setting.setName(ingressComponent.getName());

        var cardinality = new Cardinality();
        cardinality.setComponentSettings(new ArrayList<>());
        cardinality.getComponentSettings().add(setting);

        var deployment = new GraphDeployment();
        deployment.setName(graph.getName());
        deployment.setVersion(graph.getVersion());
        deployment.setCardinality(cardinality);

        this.graphController.tryDeployGraph(deployment);

        // Should be null because our adapter's component's cardinality was toggled false
        var adapter = this.testHelper.findAdapter(ingressAdapter.getName(), graph.getName());
        Assertions.assertNull(adapter);
    }

    @Test
    public void assertDeploysAdapter_isValid() throws Exception {

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
        hiddenAdapter.getEgressSettings().setHttpOutputType(HttpOutputType.POST);
        hiddenAdapter.setEndpoint("http://localhost:8080/test");
        hiddenAdapter.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));

        ingressComponent.setAdapter(ingressAdapter);
        hiddenComponent.setAdapter(hiddenAdapter);

        var edge = new GraphEdge();
        edge.setLeftAdapterName(ingressAdapter.getName());
        edge.setRightAdapterNames(new ArrayList<>());
        edge.getRightAdapterNames().add(hiddenAdapter.getName());
        graph.getEdges().add(edge);

        this.graphController.register(graph);

        var setting = new CardinalitySetting();
        setting.setEnabled(true);
        setting.setName(ingressComponent.getName());

        var cardinality = new Cardinality();
        cardinality.setComponentSettings(new ArrayList<>());
        cardinality.getComponentSettings().add(setting);

        var deployment = new GraphDeployment();
        deployment.setName(graph.getName());
        deployment.setVersion(graph.getVersion());
        deployment.setCardinality(cardinality);

        this.graphController.tryDeployGraph(deployment);

        // Should not be null because our adapter's component's cardinality was toggled true
        var adapter = this.testHelper.findAdapter(ingressAdapter.getName(), graph.getName());
        Assertions.assertNotNull(adapter);
    }
}