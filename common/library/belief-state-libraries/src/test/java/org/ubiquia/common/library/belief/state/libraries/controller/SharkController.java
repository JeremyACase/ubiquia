package org.ubiquia.common.library.belief.state.libraries.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.acl.generated.Shark;
import org.ubiquia.acl.generated.SharkEntity;
import org.ubiquia.common.library.belief.state.libraries.repository.SharkRepository;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.EntityRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.SharkRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.mapper.AbstractIngressDtoMapper;

@RestController
@RequestMapping("/ubiquia/belief-state-service/shark")
public class SharkController extends AbstractAclModelController<SharkEntity, Shark> {


    private static final Logger logger = LoggerFactory.getLogger(SharkController.class);
    @Autowired
    protected SharkRelationshipBuilder entityRelationshipBuilder;
    @Autowired
    private AbstractIngressDtoMapper<Shark, SharkEntity> ingressMapper;
    @Autowired
    private SharkRepository entityRepository;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityRelationshipBuilder<SharkEntity> getEntityRelationshipBuilder() {
        return this.entityRelationshipBuilder;
    }

    @Override
    public AbstractIngressDtoMapper<Shark, SharkEntity> getIngressMapper() {
        return this.ingressMapper;
    }

    @Override
    public SharkRepository getEntityRepository() {
        return this.entityRepository;
    }
}

