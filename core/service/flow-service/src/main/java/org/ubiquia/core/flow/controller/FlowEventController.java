package org.ubiquia.core.flow.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.library.dao.controller.GenericUbiquiaDaoController;
import org.ubiquia.common.library.implementation.service.mapper.FlowEventDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.FlowEvent;
import org.ubiquia.common.model.ubiquia.entity.FlowEventEntity;

@RestController
@RequestMapping("/ubiquia/event")
public class FlowEventController extends GenericUbiquiaDaoController<FlowEventEntity, FlowEvent> {

    private static final Logger logger = LoggerFactory.getLogger(FlowEventController.class);

    @Autowired
    private FlowEventDtoMapper dtoMapper;

    @Autowired
    private EntityDao<FlowEventEntity> entityDao;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityDao<FlowEventEntity> getDataAccessObject() {
        return this.entityDao;
    }

    @Override
    public InterfaceEntityToDtoMapper<FlowEventEntity, FlowEvent> getDataTransferObjectMapper() {
        return this.dtoMapper;
    }
}