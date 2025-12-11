package org.ubiquia.core.flow.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.library.dao.controller.GenericUbiquiaDaoController;
import org.ubiquia.common.library.implementation.service.mapper.FlowDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.Flow;
import org.ubiquia.common.model.ubiquia.entity.FlowEntity;

@RestController
@RequestMapping("/ubiquia/flow-service/flow")
public class FlowController extends GenericUbiquiaDaoController<FlowEntity, Flow> {

    private static final Logger logger = LoggerFactory.getLogger(FlowController.class);

    @Autowired
    private FlowDtoMapper dtoMapper;

    @Autowired
    private EntityDao<FlowEntity> entityDao;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityDao<FlowEntity> getDataAccessObject() {
        return this.entityDao;
    }

    @Override
    public InterfaceEntityToDtoMapper<FlowEntity, Flow> getDataTransferObjectMapper() {
        return this.dtoMapper;
    }
}