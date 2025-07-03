package org.ubiquia.common.library.belief.state.libraries.service.builder.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.acl.generated.DachschundEntity;

@Service
public class DachschundRelationshipBuilder extends EntityRelationshipBuilder<DachschundEntity> {

    protected static final Logger logger = LoggerFactory.getLogger(DachschundRelationshipBuilder.class);

    @Override
    public Logger getLogger() {
        return logger;
    }
}