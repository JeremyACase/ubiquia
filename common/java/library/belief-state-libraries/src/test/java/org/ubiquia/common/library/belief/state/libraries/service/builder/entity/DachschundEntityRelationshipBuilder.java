package org.ubiquia.common.library.belief.state.libraries.service.builder.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.domain.generated.DachschundEntity;

/** Relationship builder for dachschund entities. */
@Service
public class DachschundEntityRelationshipBuilder
    extends EntityRelationshipBuilder<DachschundEntity> {

    protected static final Logger logger =
        LoggerFactory.getLogger(DachschundEntityRelationshipBuilder.class);

    @Override
    public Logger getLogger() {
        return logger;
    }
}