package org.ubiquia.core.flow.service.io;


import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.config.GraphDeployments;
import org.ubiquia.core.flow.controller.GraphController;

/**
 * This is a service that can "bootstrap" an Ubiquia instance using graphs and Agent Communication
 * languages from file.
 */
@ConditionalOnProperty(
    value = "ubiquia.instance.bootstrap.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class BootstrapGraphDeployer {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapGraphDeployer.class);

    @Autowired
    private GraphDeployments graphDeployments;

    @Autowired
    private GraphController graphController;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Boostrap this Ubiquia instance.
     */
    public void tryDeployBootstrappedGraphs() throws Exception {
        logger.info("Deploying graphs after bootstrapping...");

        if (Objects.nonNull(this.graphDeployments)) {
            for (var deployment : this.graphDeployments.getDeployments()) {
                logger.info("...deploying graph {} with version: {}",
                    deployment.getName(),
                    deployment.getVersion());
                //this.graphController.tryDeployGraph(deployment);
            }
        }
        logger.info("...completed deployment of graphs.");
    }
}
