package org.ubiquia.core.flow.service.cluster.synchronization.entity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.library.implementation.service.mapper.GenericDtoMapper;
import org.ubiquia.common.library.implementation.service.mapper.NetworkDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.Network;
import org.ubiquia.common.model.ubiquia.entity.NetworkEntity;
import org.ubiquia.core.flow.repository.NetworkRepository;

/** Synchronizes network entities to peer agents in the cluster. */
@Service
public class NetworkSynchronizationService
    extends AbstractSynchronizationService<NetworkEntity, Network> {

    private static final String ENDPOINT_PATH =
        "/ubiquia/core/flow-service/network/register/post";

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private NetworkDtoMapper networkDtoMapper;

    @Override
    protected AbstractEntityRepository<NetworkEntity> getRepository() {
        return this.networkRepository;
    }

    @Override
    protected GenericDtoMapper<NetworkEntity, Network> getMapper() {
        return this.networkDtoMapper;
    }

    @Override
    protected String getEndpointPath() {
        return ENDPOINT_PATH;
    }
}
