package org.ubiquia.core.flow.service.io;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguageDto;
import org.ubiquia.common.model.ubiquia.dto.GraphDto;
import org.ubiquia.core.flow.service.registrar.AgentCommunicationLanguageRegistrar;
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
public class Bootstrapper {

    private static final Logger logger = LoggerFactory.getLogger(Bootstrapper.class);
    @Value("${ubiquia.agent.flowService.bootstrap.acls.enabled}")
    private Boolean isBootstrapAcls;
    @Value("${ubiquia.agent.flowService.bootstrap.acls.directory.path}")
    private String bootstrapAclsDirectory;
    @Value("${ubiquia.agent.flowService.bootstrap.graphs.directory.path}")
    private String boostrapGraphsDirectory;
    @Value("${ubiquia.agent.flowService.bootstrap.graphs.enabled}")
    private Boolean isBootstrapGraphs;
    @Autowired
    private AgentCommunicationLanguageRegistrar aclRegistrar;
    @Autowired(required = false)
    private BootstrapGraphDeployer bootstrapGraphDeployer;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private GraphRegistrar graphRegistrar;

    /**
     * Bootstrap.
     */
    public void init() {
        logger.info("Bootstrapping...");
        try {
            this.bootstrapAcls();
            this.bootstrapGraphs();
            if (Objects.nonNull(this.bootstrapGraphDeployer)) {
                this.bootstrapGraphDeployer.tryDeployBootstrappedGraphs();
            }
        } catch (Exception e) {
            logger.error("ERROR bootstrapping: {}", e.getMessage());
        }
        logger.info("...completed bootstrapping.");
    }

    /**
     * Bootstrap any domain ontologies found in the configured directory by this service.
     */
    private void bootstrapAcls() throws IOException {

        if (this.isBootstrapAcls) {
            logger.info("...bootstrapping agent communication languages (ACL's) from {}...",
                this.bootstrapAclsDirectory);
            var bootstrapPath = Paths.get(this.bootstrapAclsDirectory);

            var filePaths = Files.list(bootstrapPath).toList();
            for (var filePath : filePaths) {
                try {
                    logger.info("...bootstrapping file: {}", filePath.getFileName());
                    var acl = this.objectMapper.readValue(
                        filePath.toFile(),
                        AgentCommunicationLanguageDto.class);
                    this.aclRegistrar.tryRegister(acl);
                } catch (Exception e) {
                    logger.error("Could not not bootstrap file at filepath {}: {}",
                        bootstrapPath,
                        e.getMessage());
                }
            }
            logger.info("...completed bootstrapping agent communication languages from "
                + "directory...");
        }
    }

    /**
     * Bootstrap any graphs found by this service.
     */
    private void bootstrapGraphs() throws IOException {
        if (this.isBootstrapGraphs) {
            logger.info("...bootstrapping graphs from {}...", this.boostrapGraphsDirectory);
            var bootstrapPath = Paths.get(this.boostrapGraphsDirectory);

            var filePaths = Files.list(bootstrapPath).toList();
            var yamlMapper = new ObjectMapper(new YAMLFactory());
            for (var filePath : filePaths) {
                try {
                    logger.info("...bootstrapping file: {}", filePath.getFileName());
                    var graph = yamlMapper.readValue(filePath.toFile(), GraphDto.class);
                    this.graphRegistrar.tryRegister(graph);
                } catch (Exception e) {
                    logger.error("Could not not bootstrap file at filepath {}: {}",
                        bootstrapPath,
                        e.getMessage());
                }
            }
            logger.info("...completed bootstrapping graphs from directory...");
        }
    }
}
