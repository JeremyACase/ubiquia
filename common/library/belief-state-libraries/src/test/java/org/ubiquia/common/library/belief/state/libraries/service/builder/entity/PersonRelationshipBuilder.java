package org.ubiquia.common.library.belief.state.libraries.service.builder.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.acl.generated.entity.Person;

@Service
public class PersonRelationshipBuilder extends EntityRelationshipBuilder<Person> {

    protected static final Logger logger = LoggerFactory.getLogger(PersonRelationshipBuilder.class);

    @Override
    public Logger getLogger() {
        return logger;
    }
}