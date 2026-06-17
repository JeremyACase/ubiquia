package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.belief.state.libraries.entity.ObjectMetadataEntity;
import org.ubiquia.common.model.domain.dto.ObjectMetadataDto;

/** Ingress mapper from {@link ObjectMetadataDto} to {@link ObjectMetadataEntity}. */
@Service
public class ObjectMetadataIngressDtoMapper
    extends AbstractIngressDtoMapper<ObjectMetadataDto, ObjectMetadataEntity> {

    private static final Logger logger = LoggerFactory.getLogger(
        ObjectMetadataIngressDtoMapper.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Logger getLogger() {
        return logger;
    }

    // ObjectMetadataEntity has no domain entity relationships, so bypass the generated-class
    // lookup in the parent's tryHydrateRelationships which expects org.ubiquia.domain.generated.*
    @Override
    public ObjectMetadataEntity map(ObjectMetadataDto from, Class<ObjectMetadataEntity> toClass) {
        return this.objectMapper.convertValue(from, toClass);
    }
}
