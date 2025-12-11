package org.ubiquia.core.flow.service.io.bootstrap;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.DomainDataContract;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.core.flow.component.config.bootstrap.DomainOntologyBootstrapConfig;
import org.ubiquia.core.flow.interfaces.InterfaceBootstrapper;
import org.ubiquia.core.flow.service.registrar.DomainOntologyRegistrar;

/**
 * This is a service that can "bootstrap" a Ubiquia instance using graphs and agent communication
 * languages from file.
 */
@ConditionalOnProperty(
    value = "ubiquia.agent.flow-service.bootstrap.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class DomainOntologyBootstrapper implements InterfaceBootstrapper {

    private static final Logger logger = LoggerFactory.getLogger(DomainOntologyBootstrapper.class);

    @Autowired
    private DomainOntologyRegistrar domainOntologyRegistrar;

    @Autowired
    private DomainOntologyBootstrapConfig config;

    @Override
    public void bootstrap() throws IOException {

        logger.info("...bootstrapping domain ontologies from {}...",
            this.config.getDirectory());
        var bootstrapPath = Paths.get(this.config.getDirectory());

        var filePaths = Files.list(bootstrapPath).toList();
        var yamlMapper = new ObjectMapper(new YAMLFactory());
        for (var filePath : filePaths) {
            try {
                logger.info("...bootstrapping file: {}", filePath.getFileName());
                var domainOntology = yamlMapper.readValue(
                    filePath.toFile(),
                    DomainOntology.class);
                this.domainOntologyRegistrar.tryRegister(domainOntology);
            } catch (Exception e) {
                logger.error("Could not not bootstrap file at filepath {}: {}",
                    bootstrapPath,
                    e.getMessage());
            }
        }
        logger.info("...completed bootstrapping agent communication languages.");
    }
}
