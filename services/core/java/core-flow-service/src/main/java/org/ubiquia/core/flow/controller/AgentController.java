package org.ubiquia.core.flow.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.library.implementation.service.mapper.AgentDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.Agent;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;

/**
 * A controller that exposes a RESTful interface for agents.
 */
@RestController
@RequestMapping("/ubiquia/core/flow-service/agent")
public class AgentController implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    @Autowired
    private AgentDtoMapper dtoMapper;

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private EntityDao<AgentEntity> entityDao;

    @Override
    public Logger getLogger() {
        return logger;
    }

    /** Returns the DTO representation of this agent's own instance record. */
    @GetMapping(value = "/instance/get")
    @Transactional
    public Agent getInstance() throws JsonProcessingException {
        logger.debug("Received request for Ubiquia instance...");
        var id = this.agentConfig.getId();
        var record = this.agentRepository.findById(id);
        if (record.isEmpty()) {
            throw new RuntimeException("ERROR: Could not find instance id in database: "
                + id);
        }
        return this.dtoMapper.map(record.get());
    }

    /**
     * Accepts a registration push from a remote agent (e.g. a microweight agent pushing to this
     * Kubernetes agent). The remote agent is upserted into this agent's network so the heartbeat
     * service can monitor its reachability.
     */
    @PostMapping("/register")
    @Transactional
    public void register(@RequestBody final Agent agent) {
        logger.info("Registering remote agent: {}.", agent.getId());

        var entity = this.agentRepository.findById(agent.getId())
            .orElseGet(() -> {
                var e = new AgentEntity();
                e.setId(agent.getId());
                e.setDeployedGraphs(new ArrayList<>());
                return e;
            });

        entity.setBaseUrl(agent.getBaseUrl());
        entity.setReachable(true);

        // Place the remote agent in this agent's network so the heartbeat can reach it.
        this.agentRepository.findById(this.agentConfig.getId())
            .ifPresent(me -> entity.setNetwork(me.getNetwork()));

        this.agentRepository.save(entity);
        logger.info("Registered remote agent {} (baseUrl={}).",
            entity.getId(), entity.getBaseUrl());
    }

    /** Returns a page of graph IDs currently deployed by the specified agent. */
    @GetMapping("/{id}/get-deployed-graph-ids")
    public Page<String> getDeployedGraphIdsByAgent(
        @PathVariable("id") final String id,
        @RequestParam(value = "page", defaultValue = "0") final Integer page,
        @RequestParam(value = "size", defaultValue = "10") final Integer size) {

        logger.debug("Received request to return deployed graphs for agent ID: {}...",
            id);
        var pageable = PageRequest.of(page, size);
        return this.agentRepository.findDeployedGraphIdsById(id, pageable);
    }
}

