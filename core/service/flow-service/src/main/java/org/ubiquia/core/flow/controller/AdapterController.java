package org.ubiquia.core.flow.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.library.implementation.service.mapper.AdapterDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.Adapter;
import org.ubiquia.common.model.ubiquia.entity.AdapterEntity;

/**
 * A controller that exposes a RESTful interface for adapter.
 */
@RestController
@RequestMapping("/ubiquia/adapter")
public class AdapterController extends GenericUbiquiaDaoController<AdapterEntity, Adapter> {

    private static final Logger logger = LoggerFactory.getLogger(AdapterController.class);

    @Autowired
    private AdapterDtoMapper dtoMapper;

    @Autowired
    private EntityDao<AdapterEntity> entityDao;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityDao<AdapterEntity> getDataAccessObject() {
        return this.entityDao;
    }

    @Override
    public InterfaceEntityToDtoMapper<AdapterEntity, Adapter> getDataTransferObjectMapper() {
        return this.dtoMapper;
    }
}
