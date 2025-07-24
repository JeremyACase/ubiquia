package org.ubiquia.common.library.belief.state.libraries.service.builder.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.acl.generated.SharkEntity;

@Service
public class SharkEntityRelationshipBuilder extends EntityRelationshipBuilder<SharkEntity> {

    protected static final Logger logger = LoggerFactory.getLogger(SharkEntityRelationshipBuilder.class);

    @Override
    public Logger getLogger() {
        return logger;
    }

}