package org.ubiquia.core.flow.service.visitor;

import java.util.ArrayList;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.model.ubiquia.embeddable.Cardinality;
import org.ubiquia.common.model.ubiquia.embeddable.CardinalitySetting;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.enums.AdapterType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.service.manager.AdapterManager;


@SpringBootTest
public class AdapterCardinalityVisitorTest {

    @Autowired
    private AdapterManager adapterManager;

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

        var pushAdapter = this.dummyFactory.generateAdapter();
        pushAdapter.setAdapterType(AdapterType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        pushAdapter.getInputSubSchemas().add(subSchema);
        pushAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));

        graph.setComponentlessAdapters(new ArrayList<>());
        graph.getComponentlessAdapters().add(pushAdapter);

        this.graphController.register(graph);

        var setting = new CardinalitySetting();
        setting.setEnabled(false);
        setting.setName(pushAdapter.getName());

        var cardinality = new Cardinality();
        cardinality.setComponentlessAdapterSettings(new ArrayList<>());
        cardinality.getComponentlessAdapterSettings().add(setting);

        var deployment = new GraphDeployment();
        deployment.setName(graph.getName());
        deployment.setVersion(graph.getVersion());
        deployment.setCardinality(cardinality);

        this.graphController.tryDeployGraph(deployment);

        var adapterMap = (HashMap<String, HashMap<String, AbstractAdapter>>) ReflectionTestUtils
            .getField(this.adapterManager, "adapterMap");

        // Should be empty map since no graph (and thus no adapter) was put into our map
        Assertions.assertFalse(adapterMap.containsKey(graph.getName()));
    }

    @Test
    public void assertDeploysAdapter_isValid() throws Exception {

        var graph = this.dummyFactory.generateGraph();

        var pushAdapter = this.dummyFactory.generateAdapter();
        pushAdapter.setAdapterType(AdapterType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        pushAdapter.getInputSubSchemas().add(subSchema);
        pushAdapter.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));

        graph.setComponentlessAdapters(new ArrayList<>());
        graph.getComponentlessAdapters().add(pushAdapter);

        this.graphController.register(graph);

        var setting = new CardinalitySetting();
        setting.setEnabled(true);
        setting.setName(pushAdapter.getName());

        var cardinality = new Cardinality();
        cardinality.setComponentlessAdapterSettings(new ArrayList<>());
        cardinality.getComponentlessAdapterSettings().add(setting);

        var deployment = new GraphDeployment();
        deployment.setName(graph.getName());
        deployment.setVersion(graph.getVersion());
        deployment.setCardinality(cardinality);

        this.graphController.tryDeployGraph(deployment);

        var adapter = this.testHelper.findAdapter(pushAdapter.getName(), graph.getName());
        Assertions.assertNotNull(adapter);
    }
}