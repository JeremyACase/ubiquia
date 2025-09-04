package org.ubiquia.common.library.belief.state.libraries.service.builder.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.acl.generated.CatEntity;

@Service
public class CatEntityRelationshipBuilder extends EntityRelationshipBuilder<CatEntity> {

    protected static final Logger logger = LoggerFactory.getLogger(CatEntityRelationshipBuilder.class);

    @Override
    public Logger getLogger() {
        return logger;
    }
}