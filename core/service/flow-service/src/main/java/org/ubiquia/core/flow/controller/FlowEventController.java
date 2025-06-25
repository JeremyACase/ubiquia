package org.ubiquia.core.flow.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.controller.GenericUbiquiaDaoController;
import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.model.ubiquia.dto.FlowEventDto;
import org.ubiquia.common.model.ubiquia.entity.FlowEvent;
import org.ubiquia.core.flow.service.mapper.FlowEventDtoMapper;

@RestController
@RequestMapping("/ubiquia/event")
public class FlowEventController extends GenericUbiquiaDaoController<FlowEvent, FlowEventDto> {

    private static final Logger logger = LoggerFactory.getLogger(FlowEventController.class);

    @Autowired
    private FlowEventDtoMapper dtoMapper;

    @Autowired
    private EntityDao<FlowEvent> entityDao;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityDao<FlowEvent> getDataAccessObject() {
        return this.entityDao;
    }

    @Override
    public InterfaceEntityToDtoMapper<FlowEvent, FlowEventDto> getDataTransferObjectMapper() {
        return this.dtoMapper;
    }
}