package org.ubiquia.core.flow.controller;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.library.dao.controller.GenericUbiquiaDaoController;
import org.ubiquia.common.library.implementation.service.mapper.FlowEventDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.FlowEvent;
import org.ubiquia.common.model.ubiquia.entity.FlowEventEntity;
import org.ubiquia.core.flow.service.registrar.FlowEventRegistrar;

/** REST controller exposing DAO operations and registration for flow event entities. */
@RestController
@RequestMapping("/ubiquia/core/flow-service/flow-event")
public class FlowEventController extends GenericUbiquiaDaoController<FlowEventEntity, FlowEvent> {

    private static final Logger logger = LoggerFactory.getLogger(FlowEventController.class);

    @Autowired
    private FlowEventDtoMapper dtoMapper;

    @Autowired
    private EntityDao<FlowEventEntity> entityDao;

    @Autowired
    private FlowEventRegistrar flowEventRegistrar;

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

    /** Registers a flow event from a JSON request body. */
    @PostMapping("/register/post")
    @Transactional
    public void register(@RequestBody @Valid final FlowEvent flowEvent) throws Exception {
        this.getLogger().info("Received FlowEvent registration request for id {}.",
            flowEvent.getId());
        this.flowEventRegistrar.tryRegister(flowEvent);
    }
}