package org.ubiquia.core.flow.controller;

import jakarta.transaction.Transactional;
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
import org.ubiquia.common.library.implementation.service.mapper.NetworkDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.Network;
import org.ubiquia.common.model.ubiquia.entity.NetworkEntity;
import org.ubiquia.core.flow.service.registrar.NetworkRegistrar;

/** REST controller for registering and querying network entities. */
@RestController
@RequestMapping("/ubiquia/core/flow-service/network")
public class NetworkController extends GenericUbiquiaDaoController<NetworkEntity, Network> {

    private static final Logger logger = LoggerFactory.getLogger(NetworkController.class);

    @Autowired
    private NetworkDtoMapper dtoMapper;

    @Autowired
    private EntityDao<NetworkEntity> entityDao;

    @Autowired
    private NetworkRegistrar networkRegistrar;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityDao<NetworkEntity> getDataAccessObject() {
        return this.entityDao;
    }

    @Override
    public InterfaceEntityToDtoMapper<NetworkEntity, Network> getDataTransferObjectMapper() {
        return this.dtoMapper;
    }

    /** Registers a network from a JSON request body. */
    @PostMapping("/register/post")
    @Transactional
    public void register(@RequestBody final Network network) {
        this.getLogger().info("Received Network registration request for id {}.", network.getId());
        this.networkRegistrar.tryRegister(network);
    }
}
