package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.belief.state.libraries.entity.ObjectMetadataEntity;
import org.ubiquia.common.model.domain.dto.ObjectMetadataDto;

/** Egress mapper from {@link ObjectMetadataEntity} to {@link ObjectMetadataDto}. */
@Service
public class ObjectMetadataEgressDtoMapper
    extends AbstractEgressDtoMapper<ObjectMetadataEntity, ObjectMetadataDto> {

    private static final Logger logger = LoggerFactory.getLogger(
        ObjectMetadataEgressDtoMapper.class);

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public ObjectMetadataDto getNewDto() {
        return new ObjectMetadataDto();
    }

    @Override
    public String getModelType() {
        return "ObjectMetadata";
    }
}
