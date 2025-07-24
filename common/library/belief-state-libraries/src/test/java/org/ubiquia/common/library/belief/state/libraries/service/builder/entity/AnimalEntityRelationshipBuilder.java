package org.ubiquia.common.library.belief.state.libraries.service.builder.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.acl.generated.AnimalEntity;

@Service
public class AnimalEntityRelationshipBuilder extends EntityRelationshipBuilder<AnimalEntity> {

    protected static final Logger logger = LoggerFactory.getLogger(AnimalEntityRelationshipBuilder.class);

    @Override
    public Logger getLogger() {
        return logger;
    }
}