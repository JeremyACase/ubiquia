package org.ubiquia.core.flow.service.logic.agent;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.repository.AgentRepository;


/**
 * A service that ensures our Flow instance initializes in the appropriate order.
 */
@Service
public class AgentLogic {

    private static final Logger logger = LoggerFactory.getLogger(AgentLogic.class);

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentRepository agentRepository;

    @Transactional
    public void tryInitializeAgentInDatabase() {
        logger.info("...attempting to initialize Ubiquia agent id: {}...",
            this.agentConfig.getId());

        var record = this.agentRepository.findById(this.agentConfig.getId());
        if (record.isEmpty()) {
            logger.info("...no record found in database for agent id: {}..."
                , this.agentConfig.getId());
            logger.info("...creating new agent record...");

            var entity = new AgentEntity();
            entity.setDeployedGraphs(new ArrayList<>());
            entity.setId(this.agentConfig.getId());
            entity = this.agentRepository.save(entity);
            logger.info("...created ubiquia agent entity with id: {}", entity.getId());
        }

        logger.info("...finished initialization.");
    }
}
