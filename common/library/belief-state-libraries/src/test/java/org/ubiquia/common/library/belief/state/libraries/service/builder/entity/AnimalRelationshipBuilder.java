package org.ubiquia.common.library.belief.state.libraries.service.builder.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.acl.generated.entity.AnimalEntity;

@Service
public class AnimalRelationshipBuilder extends EntityRelationshipBuilder<AnimalEntity> {

    protected static final Logger logger = LoggerFactory.getLogger(AnimalRelationshipBuilder.class);

    @Override
    public Logger getLogger() {
        return logger;
    }
}