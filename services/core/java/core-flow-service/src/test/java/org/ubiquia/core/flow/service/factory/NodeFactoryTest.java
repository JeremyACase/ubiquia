package org.ubiquia.core.flow.service.factory;

import jakarta.transaction.Transactional;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.component.node.AbstractNode;


@SpringBootTest
@AutoConfigureMockMvc
public class NodeFactoryTest {

    @Autowired
    private NodeFactory nodeFactory;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    @Transactional
    public void assertBuildsNodes_isValid() {
        var allNodesBuild = true;
        for (var type : NodeType.values()) {
            var node = (AbstractNode) ReflectionTestUtils.invokeMethod(
                this.nodeFactory,
                "makeNodeByType",
                type);
            if (Objects.isNull(node)) {
                allNodesBuild = false;
            }
        }
        Assertions.assertTrue(allNodesBuild);
    }
}