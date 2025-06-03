package org.ubiquia.core.flow.service.manager;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.ubiquia.common.model.ubiquia.dto.GraphEdgeDto;
import org.ubiquia.common.model.ubiquia.embeddable.EgressSettings;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.enums.AdapterType;
import org.ubiquia.common.model.ubiquia.enums.AgentType;
import org.ubiquia.common.model.ubiquia.enums.HttpOutputType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;


@SpringBootTest
@AutoConfigureMockMvc
public class AgentManagerTest {

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private GraphController graphController;

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.clearAllState();
    }

    @Test
    @Transactional
    public void assertDeploysAgents_throwsIllegalArgumentException()
        throws Exception {

        var graph = this.dummyFactory.generateGraph();

        var ingressAgent = this.dummyFactory.generateAgent();
        ingressAgent.setAgentType(AgentType.POD);
        var hiddenAgent = this.dummyFactory.generateAgent();
        hiddenAgent.setAgentType(AgentType.POD);
        graph.getAgents().add(ingressAgent);
        graph.getAgents().add(hiddenAgent);

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

        var edge = new GraphEdgeDto();
        edge.setLeftAdapterName(ingressAdapter.getAdapterName());
        edge.setRightAdapterNames(new ArrayList<>());
        edge.getRightAdapterNames().add(hiddenAdapter.getAdapterName());
        graph.getEdges().add(edge);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getGraphName());
        deployment.setVersion(graph.getVersion());

        // Should throw exception since K8s is not enabled.
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            this.agentManager.tryDeployAgentsFor(
                deployment);
        });
    }

    @Test
    @Transactional
    public void assertDeploysAgents_isValid() throws Exception {
        var graph = this.dummyFactory.generateGraph();

        var ingressAgent = this.dummyFactory.generateAgent();
        ingressAgent.setAgentType(AgentType.TEMPLATE);
        var hiddenAgent = this.dummyFactory.generateAgent();
        hiddenAgent.setAgentType(AgentType.TEMPLATE);
        graph.getAgents().add(ingressAgent);
        graph.getAgents().add(hiddenAgent);

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

        var edge = new GraphEdgeDto();
        edge.setLeftAdapterName(ingressAdapter.getAdapterName());
        edge.setRightAdapterNames(new ArrayList<>());
        edge.getRightAdapterNames().add(hiddenAdapter.getAdapterName());
        graph.getEdges().add(edge);

        this.graphController.register(graph);
        var deployment = new GraphDeployment();
        deployment.setName(graph.getGraphName());
        deployment.setVersion(graph.getVersion());

        // Should throw exception since K8s is not enabled.
        Assertions.assertDoesNotThrow(() -> {
            this.agentManager.tryDeployAgentsFor(
                deployment);
        });
    }
}