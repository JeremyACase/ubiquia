package org.ubiquia.core.flow.service.cluster.synchronization.entity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.library.implementation.service.mapper.FlowMessageDtoMapper;
import org.ubiquia.common.library.implementation.service.mapper.GenericDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.common.model.ubiquia.entity.FlowMessageEntity;
import org.ubiquia.core.flow.repository.FlowMessageRepository;

/** Synchronizes flow message entities to peer agents in the cluster. */
@Service
public class FlowMessageSynchronizationService
    extends AbstractSynchronizationService<FlowMessageEntity, FlowMessage> {

    private static final String ENDPOINT_PATH =
        "/ubiquia/core/flow-service/flow-message/register/post";

    @Autowired
    private FlowMessageRepository flowMessageRepository;

    @Autowired
    private FlowMessageDtoMapper flowMessageDtoMapper;

    @Override
    protected AbstractEntityRepository<FlowMessageEntity> getRepository() {
        return this.flowMessageRepository;
    }

    @Override
    protected GenericDtoMapper<FlowMessageEntity, FlowMessage> getMapper() {
        return this.flowMessageDtoMapper;
    }

    @Override
    protected String getEndpointPath() {
        return ENDPOINT_PATH;
    }
}
