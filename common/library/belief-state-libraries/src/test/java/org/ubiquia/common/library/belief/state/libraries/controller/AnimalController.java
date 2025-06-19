package org.ubiquia.common.library.belief.state.libraries.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.acl.generated.dto.AnimalDto;
import org.ubiquia.acl.generated.entity.Animal;
import org.ubiquia.common.library.belief.state.libraries.repository.AnimalRepository;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.AnimalRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.EntityRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.mapper.AbstractIngressDtoMapper;

@RestController
@RequestMapping("/ubiquia/belief-state-service/animal")
public class AnimalController extends AbstractAclModelController<Animal, AnimalDto> {


    private static final Logger logger = LoggerFactory.getLogger(AnimalController.class);
    @Autowired
    protected AnimalRelationshipBuilder entityRelationshipBuilder;
    @Autowired
    private AbstractIngressDtoMapper<AnimalDto, Animal> ingressMapper;
    @Autowired
    private AnimalRepository entityRepository;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityRelationshipBuilder<Animal> getEntityRelationshipBuilder() {
        return this.entityRelationshipBuilder;
    }

    @Override
    public AbstractIngressDtoMapper<AnimalDto, Animal> getIngressMapper() {
        return this.ingressMapper;
    }

    @Override
    public AnimalRepository getEntityRepository() {
        return this.entityRepository;
    }
}

