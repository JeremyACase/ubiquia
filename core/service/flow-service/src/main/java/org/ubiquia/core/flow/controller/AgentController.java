package org.ubiquia.core.flow.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.controller.GenericUbiquiaDaoController;
import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.model.ubiquia.dto.AgentDto;
import org.ubiquia.common.model.ubiquia.entity.Agent;
import org.ubiquia.core.flow.service.mapper.AgentDtoMapper;

/**
 * A controller that exposes a RESTful interface for agents.
 */
@RestController
@RequestMapping("/ubiquia/agent")
public class AgentController extends GenericUbiquiaDaoController<Agent, AgentDto> {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    @Autowired
    private AgentDtoMapper dtoMapper;

    @Autowired
    private EntityDao<Agent> entityDao;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityDao<Agent> getDataAccessObject() {
        return this.entityDao;
    }

    @Override
    public InterfaceEntityToDtoMapper<Agent, AgentDto> getDataTransferObjectMapper() {
        return this.dtoMapper;
    }
}
