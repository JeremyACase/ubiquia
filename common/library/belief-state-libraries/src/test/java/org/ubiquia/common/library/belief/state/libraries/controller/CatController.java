package org.ubiquia.common.library.belief.state.libraries.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.acl.generated.dto.Cat;
import org.ubiquia.acl.generated.entity.CatEntity;
import org.ubiquia.common.library.belief.state.libraries.repository.CatRepository;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.CatRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.EntityRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.mapper.AbstractIngressDtoMapper;

@RestController
@RequestMapping("/ubiquia/belief-state-service/cat")
public class CatController extends AbstractAclModelController<CatEntity, Cat> {


    private static final Logger logger = LoggerFactory.getLogger(CatController.class);
    @Autowired
    protected CatRelationshipBuilder entityRelationshipBuilder;
    @Autowired
    private AbstractIngressDtoMapper<Cat, CatEntity> ingressMapper;
    @Autowired
    private CatRepository entityRepository;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityRelationshipBuilder<CatEntity> getEntityRelationshipBuilder() {
        return this.entityRelationshipBuilder;
    }

    @Override
    public AbstractIngressDtoMapper<Cat, CatEntity> getIngressMapper() {
        return this.ingressMapper;
    }

    @Override
    public CatRepository getEntityRepository() {
        return this.entityRepository;
    }
}

