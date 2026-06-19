package org.ubiquia.core.flow.service.cluster.synchronization.entity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.library.implementation.service.mapper.GenericDtoMapper;
import org.ubiquia.common.library.implementation.service.mapper.ObjectMetadataDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.ObjectMetadata;
import org.ubiquia.common.model.ubiquia.entity.ObjectMetadataEntity;
import org.ubiquia.core.flow.repository.ObjectMetadataRepository;

/** Synchronizes object metadata entities to peer agents in the cluster. */
@Service
public class ObjectMetadataSynchronizationService
    extends AbstractSynchronizationService<ObjectMetadataEntity, ObjectMetadata> {

    private static final String ENDPOINT_PATH =
        "/ubiquia/core/flow-service/object-metadata/register/post";

    @Autowired
    private ObjectMetadataRepository objectMetadataRepository;

    @Autowired
    private ObjectMetadataDtoMapper objectMetadataDtoMapper;

    @Override
    protected AbstractEntityRepository<ObjectMetadataEntity> getRepository() {
        return this.objectMetadataRepository;
    }

    @Override
    protected GenericDtoMapper<ObjectMetadataEntity, ObjectMetadata> getMapper() {
        return this.objectMetadataDtoMapper;
    }

    @Override
    protected String getEndpointPath() {
        return ENDPOINT_PATH;
    }
}
