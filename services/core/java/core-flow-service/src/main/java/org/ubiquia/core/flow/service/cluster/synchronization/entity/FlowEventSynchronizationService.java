package org.ubiquia.core.flow.service.cluster.synchronization.entity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.library.implementation.service.mapper.FlowEventDtoMapper;
import org.ubiquia.common.library.implementation.service.mapper.GenericDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.FlowEvent;
import org.ubiquia.common.model.ubiquia.entity.FlowEventEntity;
import org.ubiquia.core.flow.repository.FlowEventRepository;

/** Synchronizes flow event entities to peer agents in the cluster. */
@Service
public class FlowEventSynchronizationService
    extends AbstractSynchronizationService<FlowEventEntity, FlowEvent> {

    private static final String ENDPOINT_PATH =
        "/ubiquia/core/flow-service/flow-event/register/post";

    @Autowired
    private FlowEventRepository flowEventRepository;

    @Autowired
    private FlowEventDtoMapper flowEventDtoMapper;

    @Override
    protected AbstractEntityRepository<FlowEventEntity> getRepository() {
        return this.flowEventRepository;
    }

    @Override
    protected GenericDtoMapper<FlowEventEntity, FlowEvent> getMapper() {
        return this.flowEventDtoMapper;
    }

    @Override
    protected String getEndpointPath() {
        return ENDPOINT_PATH;
    }
}
