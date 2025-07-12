package org.ubiquia.core.flow.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import org.ubiquia.common.library.api.config.UbiquiaAgentConfig;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.library.api.repository.UbiquiaAgentRepository;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.library.implementation.service.mapper.UbiquiaAgentDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.UbiquiaAgent;
import org.ubiquia.common.model.ubiquia.entity.UbiquiaAgentEntity;

/**
 * A controller that exposes a RESTful interface for agents.
 */
@RestController
@RequestMapping("/ubiquia/ubiquia-agent")
public class UbiquiaAgentController implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(UbiquiaAgentController.class);

    @Autowired
    private UbiquiaAgentDtoMapper dtoMapper;

    @Autowired
    private UbiquiaAgentConfig ubiquiaAgentConfig;

    @Autowired
    private UbiquiaAgentRepository ubiquiaAgentRepository;

    @Autowired
    private EntityDao<UbiquiaAgentEntity> entityDao;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @GetMapping(value = "/instance/get")
    @Transactional
    public UbiquiaAgent getInstance() throws JsonProcessingException {
        logger.debug("Received request for Ubiquia instance...");
        var id = this.ubiquiaAgentConfig.getId();
        var record = this.ubiquiaAgentRepository.findById(id);
        if (record.isEmpty()) {
            throw new RuntimeException("ERROR: Could not find instance id in database: "
                + id);
        }
        return this.dtoMapper.map(record.get());
    }

    @GetMapping("/{id}/get-deployed-graph-ids")
    public Page<String> getDeployedGraphIdsByUbiquiaAgent(
        @PathVariable("id") final String agentId,
        @RequestParam(value = "page", defaultValue = "0") final Integer page,
        @RequestParam(value = "size", defaultValue = "10") final Integer size) {

        logger.debug("Received request to return deployed graphs for agent ID: {}...",
            agentId);
        var pageable = PageRequest.of(page, size);
        return this.ubiquiaAgentRepository.findDeployedGraphIdsById(agentId, pageable);
    }
}

