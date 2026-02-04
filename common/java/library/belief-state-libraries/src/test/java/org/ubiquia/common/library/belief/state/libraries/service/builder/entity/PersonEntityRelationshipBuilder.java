package org.ubiquia.common.library.belief.state.libraries.service.builder.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.domain.generated.PersonEntity;

@Service
public class PersonEntityRelationshipBuilder extends EntityRelationshipBuilder<PersonEntity> {

    protected static final Logger logger = LoggerFactory.getLogger(PersonEntityRelationshipBuilder.class);

    @Override
    public Logger getLogger() {
        return logger;
    }
}