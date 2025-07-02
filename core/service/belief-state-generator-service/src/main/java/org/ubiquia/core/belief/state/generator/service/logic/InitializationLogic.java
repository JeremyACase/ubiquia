package org.ubiquia.core.belief.state.generator.service.logic;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.ubiquia.core.belief.state.generator.service.k8s.BeliefStateOperator;

@Service
public class InitializationLogic implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(InitializationLogic.class);

    @Autowired(required = false)
    private BeliefStateOperator beliefStateOperator;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("Initializing Belief State Generator instance...");

        if (Objects.nonNull(this.beliefStateOperator)) {
            try {
                this.beliefStateOperator.init();
            } catch (Exception e) {
                logger.error("ERROR - could not initialize: {}", e.getMessage());
            }
        }

        logger.info("...completed initialization.");
    }
}
