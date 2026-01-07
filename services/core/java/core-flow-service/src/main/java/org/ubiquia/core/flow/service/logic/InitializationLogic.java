package org.ubiquia.core.flow.service.logic;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.service.io.bootstrap.BeliefStateBootstrapper;
import org.ubiquia.core.flow.service.io.bootstrap.DomainOntologyBootstrapper;
import org.ubiquia.core.flow.service.k8s.ComponentOperator;
import org.ubiquia.core.flow.service.logic.agent.AgentLogic;


/**
 * A service that ensures our Flow instance initializes in the appropriate order.
 */
@Service
public class InitializationLogic implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(InitializationLogic.class);

    @Autowired(required = false)
    private DomainOntologyBootstrapper domainOntologyBootstrapper;

    @Autowired(required = false)
    private BeliefStateBootstrapper beliefStateBootstrapper;

    @Autowired(required = false)
    private ComponentOperator componentOperator;

    @Autowired
    private AgentLogic agentLogic;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("Initializing Flow instance...");

        this.agentLogic.tryInitializeAgentInDatabase();

        if (Objects.nonNull(this.componentOperator)) {
            try {
                this.componentOperator.init();
            } catch (Exception e) {
                logger.error("ERROR - could not initialize: {}", e.getMessage());
            }
        }

        if (Objects.nonNull(this.domainOntologyBootstrapper)) {
            try {
                this.domainOntologyBootstrapper.bootstrap();
            } catch (Exception e) {
                logger.error("ERROR bootstrapping domain ontolgoy - could not initialize: {}",
                    e.getMessage());
            }
        }

        if (Objects.nonNull(this.beliefStateBootstrapper)) {
            try {
                this.beliefStateBootstrapper.bootstrap();
            } catch (Exception e) {
                logger.error("ERROR bootstrapping belief state - could not initialize: {}",
                    e.getMessage());
            }
        }

        logger.info("...completed initialization.");
    }
}
