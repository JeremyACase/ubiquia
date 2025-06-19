package org.ubiquia.common.library.belief.state.libraries.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.acl.generated.dto.DogDto;
import org.ubiquia.acl.generated.entity.Dog;
import org.ubiquia.common.library.belief.state.libraries.repository.DogRepository;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.DogRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.EntityRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.mapper.AbstractIngressDtoMapper;

@RestController
@RequestMapping("/ubiquia/belief-state-service/dog")
public class DogController extends AbstractAclModelController<Dog, DogDto> {


    private static final Logger logger = LoggerFactory.getLogger(DogController.class);
    @Autowired
    protected DogRelationshipBuilder entityRelationshipBuilder;
    @Autowired
    private AbstractIngressDtoMapper<DogDto, Dog> ingressMapper;
    @Autowired
    private DogRepository entityRepository;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityRelationshipBuilder<Dog> getEntityRelationshipBuilder() {
        return this.entityRelationshipBuilder;
    }

    @Override
    public AbstractIngressDtoMapper<DogDto, Dog> getIngressMapper() {
        return this.ingressMapper;
    }

    @Override
    public DogRepository getEntityRepository() {
        return this.entityRepository;
    }
}

