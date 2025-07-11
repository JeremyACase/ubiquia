package org.ubiquia.common.library.belief.state.libraries.service.builder.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.acl.generated.DogEntity;

@Service
public class DogRelationshipBuilder extends EntityRelationshipBuilder<DogEntity> {

    protected static final Logger logger = LoggerFactory.getLogger(DogRelationshipBuilder.class);

    @Override
    public Logger getLogger() {
        return logger;
    }

}