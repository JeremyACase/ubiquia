package org.ubiquia.common.library.belief.state.libraries.service.builder.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.belief.state.libraries.entity.ObjectMetadataEntity;

/** Relationship builder for {@link ObjectMetadataEntity}; no domain relationships to wire. */
@Service
public class ObjectMetadataEntityRelationshipBuilder
    extends EntityRelationshipBuilder<ObjectMetadataEntity> {

    private static final Logger logger = LoggerFactory.getLogger(
        ObjectMetadataEntityRelationshipBuilder.class);

    @Override
    public Logger getLogger() {
        return logger;
    }
}
