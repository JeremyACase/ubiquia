package org.ubiquia.core.flow.service.io.bootstrap;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.core.flow.component.config.bootstrap.GraphBootstrapConfig;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.interfaces.InterfaceBootstrapper;
import org.ubiquia.core.flow.service.registrar.GraphRegistrar;

/**
 * This is a service that can "bootstrap" a Ubiquia instance using graphs and agent communication
 * languages from file.
 */
@ConditionalOnProperty(
    value = "ubiquia.agent.flowService.bootstrap.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class DagBootstrapper implements InterfaceBootstrapper {

    private static final Logger logger = LoggerFactory.getLogger(DagBootstrapper.class);

    @Autowired
    private GraphBootstrapConfig config;
    @Autowired
    private GraphController graphController;
    @Autowired
    private GraphRegistrar graphRegistrar;

    @Override
    public void bootstrap() throws Exception {
        logger.info("...bootstrapping graphs from {}...", this.config.getDirectory());
        var bootstrapPath = Paths.get(this.config.getDirectory());

        var filePaths = Files.list(bootstrapPath).toList();
        var yamlMapper = new ObjectMapper(new YAMLFactory());
        for (var filePath : filePaths) {
            try {
                logger.info("...bootstrapping file: {}", filePath.getFileName());
                var graph = yamlMapper.readValue(filePath.toFile(), Graph.class);
                this.graphRegistrar.tryRegister(graph);
            } catch (Exception e) {
                logger.error("Could not not bootstrap file at filepath {}: {}",
                    bootstrapPath,
                    e.getMessage());
            }
        }

        for (var deployment : this.config.getDeployments()) {
            logger.info("...deploying graph {} with version: {}",
                deployment.getName(),
                deployment.getVersion());
            this.graphController.tryDeployGraph(deployment);
        }

        logger.info("...completed bootstrapping graphs.");
    }
}
