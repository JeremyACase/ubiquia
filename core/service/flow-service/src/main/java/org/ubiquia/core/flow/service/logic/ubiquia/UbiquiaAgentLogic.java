package org.ubiquia.core.flow.service.logic.ubiquia;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.entity.UbiquiaAgentEntity;
import org.ubiquia.common.library.config.UbiquiaAgentConfig;
import org.ubiquia.core.flow.repository.UbiquiaAgentRepository;


/**
 * A service that ensures our Flow instance initializes in the appropriate order.
 */
@Service
public class UbiquiaAgentLogic {

    private static final Logger logger = LoggerFactory.getLogger(UbiquiaAgentLogic.class);

    @Autowired
    private UbiquiaAgentConfig ubiquiaAgentConfig;

    @Autowired
    private UbiquiaAgentRepository ubiquiaAgentRepository;

    @Transactional
    public void tryInitializeAgentInDatabase() {
        logger.info("...attempting to initialize Ubiquia agent id: {}...",
            this.ubiquiaAgentConfig.getId());

        var record = this.ubiquiaAgentRepository.findById(this.ubiquiaAgentConfig.getId());
        if (record.isEmpty()) {
            logger.info("...no record found in database for agent id: {}..."
                , this.ubiquiaAgentConfig.getId());
            logger.info("...creating new agent record...");

            var entity = new UbiquiaAgentEntity();
            entity.setDeployedGraphs(new ArrayList<>());
            entity.setId(this.ubiquiaAgentConfig.getId());
            entity = this.ubiquiaAgentRepository.save(entity);
            logger.info("...created ubiquia agent entity with id: {}", entity.getId());
        }

        logger.info("...finished initialization.");
    }
}
