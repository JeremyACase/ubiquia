package org.ubiquia.core.flow.controller;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.model.ubiquia.entity.UpdateEntity;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;


@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class UpdateControllerTest {

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private DomainOntologyController domainOntologyController;

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private EntityDao<UpdateEntity> updateEntityDao;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    @Transactional
    public void assertHasUpdateEntity() throws Exception {

        var domainOntology = this.dummyFactory.generateDomainOntology();

        var response = this
            .domainOntologyController
            .register(domainOntology);

        var domainOntologyDto = this
            .domainOntologyController
            .queryModelWithId(response.getId());

        var params = new HashMap<String, String[]>();
        var list = new String[1];
        list[0] = domainOntologyDto.getBody().getId();
        params.put("model.id", list);

        var records = this
            .updateEntityDao
            .getPage(params, 0, 1, false, new ArrayList<>(), UpdateEntity.class);

        Assertions.assertTrue(records.getContent().size() == 1);
        Assertions.assertEquals(
            this.agentConfig.getId(),
            records.getContent().get(0).getAgent().getId());
    }
}