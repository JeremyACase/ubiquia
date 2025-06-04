package org.ubiquia.core.flow.service.logic;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.service.io.Bootstrapper;
import org.ubiquia.core.flow.service.k8s.AgentOperator;
import org.ubiquia.core.flow.service.logic.ubiquia.UbiquiaAgentLogic;


/**
 * A service that ensures our Flow instance initializes in the appropriate order.
 */
@Service
public class InitializationLogic implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(InitializationLogic.class);

    @Autowired(required = false)
    private Bootstrapper bootstrapper;

    @Autowired(required = false)
    private AgentOperator agentOperator;

    @Autowired
    private UbiquiaAgentLogic ubiquiaAgentLogic;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("Initializing Flow instance...");

        this.ubiquiaAgentLogic.tryInitializeAgentInDatabase();

        if (Objects.nonNull(this.agentOperator)) {
            try {
                this.agentOperator.init();
            } catch (Exception e) {
                logger.error("ERROR - could not initialize: {}", e.getMessage());
            }
        }

        if (Objects.nonNull(this.bootstrapper)) {
            this.bootstrapper.init();
        }

        logger.info("...completed initialization.");
    }
}
