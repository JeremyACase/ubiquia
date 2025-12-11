package org.ubiquia.core.flow.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.library.dao.controller.GenericUbiquiaDaoController;
import org.ubiquia.common.library.implementation.service.mapper.NodeDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.entity.NodeEntity;

/**
 * A controller that exposes a RESTful interface for adapter.
 */
@RestController
@RequestMapping("/ubiquia/flow-service/node")
public class NodeController extends GenericUbiquiaDaoController<NodeEntity, Node> {

    private static final Logger logger = LoggerFactory.getLogger(NodeController.class);

    @Autowired
    private NodeDtoMapper dtoMapper;

    @Autowired
    private EntityDao<NodeEntity> entityDao;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityDao<NodeEntity> getDataAccessObject() {
        return this.entityDao;
    }

    @Override
    public InterfaceEntityToDtoMapper<NodeEntity, Node> getDataTransferObjectMapper() {
        return this.dtoMapper;
    }
}
