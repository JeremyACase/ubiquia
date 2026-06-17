package org.ubiquia.common.library.belief.state.libraries.service.builder.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.domain.generated.DogEntity;

/** Relationship builder for dog entities. */
@Service
public class DogEntityRelationshipBuilder extends EntityRelationshipBuilder<DogEntity> {

    protected static final Logger logger =
        LoggerFactory.getLogger(DogEntityRelationshipBuilder.class);

    @Override
    public Logger getLogger() {
        return logger;
    }

}