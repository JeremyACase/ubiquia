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
import org.ubiquia.common.model.ubiquia.embeddable.EgressSettings;
import org.ubiquia.common.model.ubiquia.embeddable.NodeSettings;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;
import org.ubiquia.common.model.ubiquia.enums.HttpOutputType;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.component.node.HiddenNode;
import org.ubiquia.core.flow.controller.DomainOntologyController;
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
    private DomainOntologyController domainOntologyController;

    @Autowired
    private FlowEventBuilder flowEventBuilder;

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

        var domainOntology = this.dummyFactory.generateDomainOntology();
        var graph = domainOntology.getGraphs().get(0);

        var ingressComponent = this.dummyFactory.generateComponent();
        var hiddenComponent = this.dummyFactory.generateComponent();
        hiddenComponent.setComponentType(ComponentType.NONE);
        graph.getComponents().add(ingressComponent);
        graph.getComponents().add(hiddenComponent);

        var ingressNode = this.dummyFactory.generateNode();
        ingressNode.setNodeType(NodeType.PUSH);
        var subSchema = this.dummyFactory.buildSubSchema("Person");
        ingressNode.getInputSubSchemas().add(subSchema);
        ingressNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Dog"));
        graph.getNodes().add(ingressNode);
        ingressComponent.setNode(ingressNode);

        var hiddenNode = this.dummyFactory.generateNode();
        hiddenNode.setNodeType(NodeType.HIDDEN);
        hiddenNode.setEgressSettings(new EgressSettings());
        hiddenNode.getEgressSettings().setHttpOutputType(HttpOutputType.PUT);
        hiddenNode.setNodeSettings(new NodeSettings());
        hiddenNode.getNodeSettings().setValidateInputPayload(true);
        hiddenNode.getNodeSettings().setPersistOutputPayload(true);
        hiddenNode.setEndpoint("http://localhost:8080/test");
        hiddenNode.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        hiddenNode.setOutputSubSchema(this.dummyFactory.buildSubSchema("Cat"));
        graph.getNodes().add(hiddenNode);
        hiddenComponent.setNode(hiddenNode);

        var edge = new GraphEdge();
        edge.setLeftNodeName(ingressNode.getName());
        edge.setRightNodeNames(new ArrayList<>());
        edge.getRightNodeNames().add(hiddenNode.getName());
        graph.getEdges().add(edge);

        this.testHelper.registerAndDeploy(domainOntology, graph);

        var node = (HiddenNode) this
            .testHelper
            .findNode(hiddenNode.getName(), graph.getName());

        var flowEvent = this
            .flowEventBuilder
            .makeFlowAndEventFrom("test", node);

        this.templateComponentProxy.proxyAsComponentWith(flowEvent);
        var fuzzyData = this
            .objectMapper
            .readValue(flowEvent.getOutputPayload(), Object.class);

        Assertions.assertDoesNotThrow(() -> {
            this.payloadModelValidator.tryValidateOutputPayloadFor(
                this.objectMapper.writeValueAsString(fuzzyData),
                node);
        });
    }
}