package org.ubiquia.core.flow.service.visitor.validator;

import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.embeddable.EgressSettings;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;

/** Test class for NodeValidator. */
@SpringBootTest
@AutoConfigureMockMvc
public class NodeValidatorTest {

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private NodeValidator nodeValidator;

    @Test
    public void assertNodeWithTargetGraph_isValid() {
        var node = this.buildValidHiddenNode();
        node.setTargetGraph(new Graph());

        Assertions.assertDoesNotThrow(() -> this.nodeValidator.tryValidateHiddenNode(node));
    }

    @Test
    public void assertNodeWithBothTargetGraphAndTargetComponent_throwsException() {
        var node = this.buildValidHiddenNode();
        node.setTargetGraph(new Graph());
        node.setTargetComponent(this.dummyFactory.generateComponent());

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> this.nodeValidator.tryValidateHiddenNode(node));
    }

    private Node buildValidHiddenNode() {
        var node = this.dummyFactory.generateNode();
        node.setNodeType(NodeType.HIDDEN);
        node.setEgressSettings(new EgressSettings());
        node.setInputSubSchemas(new ArrayList<>());
        node.getInputSubSchemas().add(this.dummyFactory.buildSubSchema("Dog"));
        node.setUpstreamNodes(new ArrayList<>());
        node.getUpstreamNodes().add(this.dummyFactory.generateNode());
        node.setDownstreamNodes(new ArrayList<>());
        return node;
    }
}
