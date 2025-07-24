package org.ubiquia.common.library.belief.state.libraries.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.acl.generated.Animal;
import org.ubiquia.acl.generated.AnimalEntity;
import org.ubiquia.common.library.belief.state.libraries.repository.AnimalEntityRepository;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.AnimalEntityRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.EntityRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.mapper.AbstractIngressDtoMapper;

@RestController
@RequestMapping("/ubiquia/belief-state-service/animal")
public class AnimalController extends AbstractAclModelController<AnimalEntity, Animal> {


    private static final Logger logger = LoggerFactory.getLogger(AnimalController.class);
    @Autowired
    protected AnimalEntityRelationshipBuilder entityRelationshipBuilder;
    @Autowired
    private AbstractIngressDtoMapper<Animal, AnimalEntity> ingressMapper;
    @Autowired
    private AnimalEntityRepository entityRepository;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityRelationshipBuilder<AnimalEntity> getEntityRelationshipBuilder() {
        return this.entityRelationshipBuilder;
    }

    @Override
    public AbstractIngressDtoMapper<Animal, AnimalEntity> getIngressMapper() {
        return this.ingressMapper;
    }

    @Override
    public AnimalEntityRepository getEntityRepository() {
        return this.entityRepository;
    }
}

