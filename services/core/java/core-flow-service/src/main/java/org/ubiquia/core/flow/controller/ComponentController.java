package org.ubiquia.core.flow.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.library.dao.controller.GenericUbiquiaDaoController;
import org.ubiquia.common.library.implementation.service.mapper.ComponentDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.Component;
import org.ubiquia.common.model.ubiquia.entity.ComponentEntity;

/**
 * A controller that exposes a RESTful interface for components within a DAG.
 */
@RestController
@RequestMapping("/ubiquia/flow-service/component")
public class ComponentController extends GenericUbiquiaDaoController<ComponentEntity, Component> {

    private static final Logger logger = LoggerFactory.getLogger(ComponentController.class);

    @Autowired
    private ComponentDtoMapper dtoMapper;

    @Autowired
    private EntityDao<ComponentEntity> entityDao;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityDao<ComponentEntity> getDataAccessObject() {
        return this.entityDao;
    }

    @Override
    public InterfaceEntityToDtoMapper<ComponentEntity, Component> getDataTransferObjectMapper() {
        return this.dtoMapper;
    }
}
