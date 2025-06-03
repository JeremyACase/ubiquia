package org.ubiquia.core.flow.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.controller.GenericUbiquiaDaoController;
import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.model.ubiquia.dto.AdapterDto;
import org.ubiquia.common.model.ubiquia.entity.Adapter;
import org.ubiquia.core.flow.service.mapper.AdapterDtoMapper;

/**
 * A controller that exposes a RESTful interface for adapter.
 */
@RestController
@RequestMapping("/ubiquia/adapter")
public class AdapterController extends GenericUbiquiaDaoController<Adapter, AdapterDto> {

    private static final Logger logger = LoggerFactory.getLogger(AdapterController.class);

    @Autowired
    private AdapterDtoMapper dtoMapper;

    @Autowired
    private EntityDao<Adapter> entityDao;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityDao<Adapter> getDataAccessObject() {
        return this.entityDao;
    }

    @Override
    public InterfaceEntityToDtoMapper<Adapter, AdapterDto> getDataTransferObjectMapper() {
        return this.dtoMapper;
    }
}
