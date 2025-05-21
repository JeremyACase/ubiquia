package org.ubiquia.core.flow.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.core.flow.model.dto.AgentDto;
import org.ubiquia.core.flow.model.entity.Agent;

/**
 * A controller that exposes a RESTful interface for agents.
 */
@RestController
@RequestMapping("/ubiquia/agent")
public class AgentController extends GenericEntityController<Agent, AgentDto> {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    @Override
    public Logger getLogger() {
        return logger;
    }
}
