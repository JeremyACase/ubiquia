package org.ubiquia.common.library.belief.state.libraries.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.acl.generated.Person;
import org.ubiquia.acl.generated.PersonEntity;
import org.ubiquia.common.library.belief.state.libraries.repository.PersonEntityRepository;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.EntityRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.PersonEntityRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.mapper.AbstractIngressDtoMapper;

@RestController
@RequestMapping("/ubiquia/belief-state-service/person")
public class PersonController extends AbstractAclModelController<PersonEntity, Person> {


    private static final Logger logger = LoggerFactory.getLogger(PersonController.class);
    @Autowired
    protected PersonEntityRelationshipBuilder entityRelationshipBuilder;
    @Autowired
    private AbstractIngressDtoMapper<Person, PersonEntity> ingressMapper;
    @Autowired
    private PersonEntityRepository entityRepository;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityRelationshipBuilder<PersonEntity> getEntityRelationshipBuilder() {
        return this.entityRelationshipBuilder;
    }

    @Override
    public AbstractIngressDtoMapper<Person, PersonEntity> getIngressMapper() {
        return this.ingressMapper;
    }

    @Override
    public PersonEntityRepository getEntityRepository() {
        return this.entityRepository;
    }
}

