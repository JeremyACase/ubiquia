package org.ubiquia.core.flow.service.io;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.model.dto.AgentCommunicationLanguageDto;
import org.ubiquia.core.flow.model.dto.GraphDto;
import org.ubiquia.core.flow.service.registrar.AgentCommunicationLanguageRegistrar;
import org.ubiquia.core.flow.service.registrar.GraphRegistrar;

/**
 * This is a service that can "bootstrap" a Ubiquia instance using graphs and agent communication
 * languages from file.
 */
@ConditionalOnProperty(
    value = "flow.bootstrap.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class Bootstrapper {

    private static final Logger logger = LoggerFactory.getLogger(Bootstrapper.class);
    @Value("${flow.bootstrap.graph.directory.path}")
    private String boostrapGraphsDirectory;
    @Value("${flow.bootstrap.graph.directory.enabled}")
    private Boolean isBootstrapGraphsFromDirectory;
    @Value("${flow.bootstrap.ontology.filepath}")
    private String boostrapOntologiesFilepath;
    @Autowired
    private AgentCommunicationLanguageRegistrar domainOntologyRegistrar;
    @Autowired(required = false)
    private BootstrapGraphDeployer bootstrapGraphDeployer;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private GraphRegistrar graphRegistrar;

    /**
     * Boostrap.
     */
    public void init() {
        logger.info("Bootstrapping...");
        try {
            this.bootstrapOntologies();
            this.bootstrapGraphsFromDirectory();
            this.bootstrapGraphDeployer.tryDeployBootstrappedGraphs();
        } catch (Exception e) {
            logger.error("ERROR bootstrapping: {}", e.getMessage());
        }
        logger.info("...completed bootstrapping.");
    }

    /**
     * Bootstrap.
     */
    private void bootstrapOntologies() {
        this.bootstrapOntologiesFromFilepath();
    }

    /**
     * Bootstrap any domain ontologies found in the configured directory by this service.
     */
    private void bootstrapOntologiesFromFilepath() {
        logger.info("...bootstrapping ontologies from filepath...");
        var bootstrapPath = Paths.get(this.boostrapOntologiesFilepath);

        if (Files.isDirectory(bootstrapPath)) {
            logger.warn("WARNING: Could not bootstrap ontologies from path {}; it is a directory",
                this.boostrapOntologiesFilepath);
        } else {
            var file = bootstrapPath.toFile();
            try {
                logger.info("...bootstrapping file: {}",
                    file.getName());
                var ontology = this.objectMapper.readValue(
                    file,
                    AgentCommunicationLanguageDto.class);
                this.domainOntologyRegistrar.tryRegister(ontology);
            } catch (Exception e) {
                logger.error("Could not not bootstrap file at filepath {}: {}",
                    file.getPath(),
                    e.getMessage());
            }
        }
        logger.info("...completed bootstrapping ontologies from directory...");
    }

    /**
     * Bootstrap any graphs found by this service.
     */
    private void bootstrapGraphsFromDirectory() throws IOException {
        if (this.isBootstrapGraphsFromDirectory) {
            logger.info("...bootstrapping graphs from directory...");
            var bootstrapPath = Paths.get(this.boostrapGraphsDirectory);

            var filePaths = Files.list(bootstrapPath).toList();
            for (var filePath : filePaths) {
                try {
                    logger.info("...bootstrapping file: {}", filePath.getFileName());
                    var yamlMapper = new ObjectMapper(new YAMLFactory());
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
