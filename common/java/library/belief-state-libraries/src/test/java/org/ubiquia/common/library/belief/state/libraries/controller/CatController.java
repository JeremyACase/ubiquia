package org.ubiquia.common.library.belief.state.libraries.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.domain.generated.Cat;
import org.ubiquia.domain.generated.CatEntity;
import org.ubiquia.common.library.belief.state.libraries.repository.CatEntityRepository;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.CatEntityRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.EntityRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.mapper.AbstractIngressDtoMapper;

@RestController
@RequestMapping("/ubiquia/belief-state-service/cat")
public class CatController extends AbstractDomainModelController<CatEntity, Cat> {


    private static final Logger logger = LoggerFactory.getLogger(CatController.class);
    @Autowired
    protected CatEntityRelationshipBuilder entityRelationshipBuilder;
    @Autowired
    private AbstractIngressDtoMapper<Cat, CatEntity> ingressMapper;
    @Autowired
    private CatEntityRepository entityRepository;

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
    public CatEntityRepository getEntityRepository() {
        return this.entityRepository;
    }
}

