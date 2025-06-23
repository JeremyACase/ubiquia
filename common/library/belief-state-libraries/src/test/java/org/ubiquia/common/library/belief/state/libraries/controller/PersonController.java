package org.ubiquia.common.library.belief.state.libraries.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.acl.generated.dto.Person;
import org.ubiquia.acl.generated.entity.PersonModel;
import org.ubiquia.common.library.belief.state.libraries.repository.PersonRepository;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.EntityRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.PersonRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.mapper.AbstractIngressDtoMapper;

@RestController
@RequestMapping("/ubiquia/belief-state-service/person")
public class PersonController extends AbstractAclModelController<PersonModel, Person> {


    private static final Logger logger = LoggerFactory.getLogger(PersonController.class);
    @Autowired
    protected PersonRelationshipBuilder entityRelationshipBuilder;
    @Autowired
    private AbstractIngressDtoMapper<Person, PersonModel> ingressMapper;
    @Autowired
    private PersonRepository entityRepository;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityRelationshipBuilder<PersonModel> getEntityRelationshipBuilder() {
        return this.entityRelationshipBuilder;
    }

    @Override
    public AbstractIngressDtoMapper<Person, PersonModel> getIngressMapper() {
        return this.ingressMapper;
    }

    @Override
    public PersonRepository getEntityRepository() {
        return this.entityRepository;
    }
}

