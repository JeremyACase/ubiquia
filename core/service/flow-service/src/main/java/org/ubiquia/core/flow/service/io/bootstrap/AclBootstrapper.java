package org.ubiquia.core.flow.service.io.bootstrap;


import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguage;
import org.ubiquia.core.flow.component.config.bootstrap.AclBootstrapConfig;
import org.ubiquia.core.flow.interfaces.InterfaceBootstrapper;
import org.ubiquia.core.flow.service.registrar.AgentCommunicationLanguageRegistrar;

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
public class AclBootstrapper implements InterfaceBootstrapper {

    private static final Logger logger = LoggerFactory.getLogger(AclBootstrapper.class);

    @Autowired
    private AgentCommunicationLanguageRegistrar aclRegistrar;

    @Autowired
    private AclBootstrapConfig config;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void bootstrap() throws IOException {

        logger.info("...bootstrapping agent communication languages (ACL's) from {}...",
            this.config.getDirectory());
        var bootstrapPath = Paths.get(this.config.getDirectory());

        var filePaths = Files.list(bootstrapPath).toList();
        for (var filePath : filePaths) {
            try {
                logger.info("...bootstrapping file: {}", filePath.getFileName());
                var acl = this.objectMapper.readValue(
                    filePath.toFile(),
                    AgentCommunicationLanguage.class);
                this.aclRegistrar.tryRegister(acl);
            } catch (Exception e) {
                logger.error("Could not not bootstrap file at filepath {}: {}",
                    bootstrapPath,
                    e.getMessage());
            }
        }
        logger.info("...completed bootstrapping agent communication languages.");
    }
}
