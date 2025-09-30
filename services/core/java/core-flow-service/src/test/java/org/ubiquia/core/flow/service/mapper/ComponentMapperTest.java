package org.ubiquia.core.flow.service.mapper;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.ubiquia.common.library.implementation.service.mapper.ComponentDtoMapper;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;
import org.ubiquia.core.flow.repository.ComponentRepository;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ComponentMapperTest {

    @Autowired
    private ComponentDtoMapper componentDtoMapper;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private GraphController graphController;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    /**
     * We want to make sure that order of post start scripts are preserved when
     * being inserted/egressed to/from the database.
     *
     * @throws Exception Exceptions from stuff.
     */
    @Test
    @Transactional
    public void assertEgressOrderMatchesIngress_isValid() throws Exception {

        var graph = this.dummyFactory.generateGraph();

        var postExecCommands = new ArrayList<String>();
        postExecCommands.add("0");
        postExecCommands.add("1");
        postExecCommands.add("2");

        var ingressComponent = this.dummyFactory.generateComponent();
        ingressComponent.setPostStartExecCommands(postExecCommands);
        graph.getComponents().add(ingressComponent);

        var response = this.graphController.register(graph);

        var graphDto = this.graphController.queryModelWithId(response.getId());
        var componentDto = graphDto.getBody().getComponents().get(0);

        Assertions.assertEquals("0", componentDto.getPostStartExecCommands().get(0));
        Assertions.assertEquals("1", componentDto.getPostStartExecCommands().get(1));
        Assertions.assertEquals("2", componentDto.getPostStartExecCommands().get(2));
    }
}
