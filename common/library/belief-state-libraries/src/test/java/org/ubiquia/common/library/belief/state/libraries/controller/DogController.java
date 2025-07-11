package org.ubiquia.common.library.belief.state.libraries.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.acl.generated.Dog;
import org.ubiquia.acl.generated.DogEntity;
import org.ubiquia.common.library.belief.state.libraries.repository.DogRepository;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.DogRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.EntityRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.mapper.AbstractIngressDtoMapper;

@RestController
@RequestMapping("/ubiquia/belief-state-service/dog")
public class DogController extends AbstractAclModelController<DogEntity, Dog> {


    private static final Logger logger = LoggerFactory.getLogger(DogController.class);
    @Autowired
    protected DogRelationshipBuilder entityRelationshipBuilder;
    @Autowired
    private AbstractIngressDtoMapper<Dog, DogEntity> ingressMapper;
    @Autowired
    private DogRepository entityRepository;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityRelationshipBuilder<DogEntity> getEntityRelationshipBuilder() {
        return this.entityRelationshipBuilder;
    }

    @Override
    public AbstractIngressDtoMapper<Dog, DogEntity> getIngressMapper() {
        return this.ingressMapper;
    }

    @Override
    public DogRepository getEntityRepository() {
        return this.entityRepository;
    }
}

