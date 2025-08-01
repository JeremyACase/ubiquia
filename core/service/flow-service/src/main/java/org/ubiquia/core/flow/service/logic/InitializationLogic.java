package org.ubiquia.core.flow.service.logic;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.service.io.bootstrap.AclBootstrapper;
import org.ubiquia.core.flow.service.io.bootstrap.BeliefStateBootstrapper;
import org.ubiquia.core.flow.service.io.bootstrap.DagBootstrapper;
import org.ubiquia.core.flow.service.k8s.ComponentOperator;
import org.ubiquia.core.flow.service.logic.ubiquia.UbiquiaAgentLogic;


/**
 * A service that ensures our Flow instance initializes in the appropriate order.
 */
@Service
public class InitializationLogic implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(InitializationLogic.class);

    @Autowired(required = false)
    private AclBootstrapper aclBootstrapper;

    @Autowired(required = false)
    private BeliefStateBootstrapper beliefStateBootstrapper;

    @Autowired(required = false)
    private DagBootstrapper dagBootstrapper;

    @Autowired(required = false)
    private ComponentOperator componentOperator;

    @Autowired
    private UbiquiaAgentLogic ubiquiaAgentLogic;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("Initializing Flow instance...");

        this.ubiquiaAgentLogic.tryInitializeAgentInDatabase();

        if (Objects.nonNull(this.componentOperator)) {
            try {
                this.componentOperator.init();
            } catch (Exception e) {
                logger.error("ERROR - could not initialize: {}", e.getMessage());
            }
        }

        if (Objects.nonNull(this.aclBootstrapper)) {
            try {
                this.aclBootstrapper.bootstrap();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (Objects.nonNull(this.beliefStateBootstrapper)) {
            try {
                this.beliefStateBootstrapper.bootstrap();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (Objects.nonNull(this.dagBootstrapper)) {
            try {
                this.dagBootstrapper.bootstrap();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        logger.info("...completed initialization.");
    }
}
