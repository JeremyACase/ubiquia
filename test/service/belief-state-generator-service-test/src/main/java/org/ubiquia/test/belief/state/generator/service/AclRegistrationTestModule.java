package org.ubiquia.test.belief.state.generator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguage;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;
import org.ubiquia.common.test.helm.component.GenericUbiquiaPostAndRetriever;
import org.ubiquia.common.test.helm.service.AbstractHelmTestModule;

@Service
public class AclRegistrationTestModule extends AbstractHelmTestModule {

    private static final Logger logger = LoggerFactory.getLogger(AclRegistrationTestModule.class);

    @Autowired
    private Cache cache;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Autowired
    private GenericUbiquiaPostAndRetriever<AgentCommunicationLanguage> postAndRetriever;

    @Value("${ubiquia.test.acl.schema.filepath}")
    private String schemaFilepath;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void doSetup() {
        logger.info("Proceeding to read in an ACL...");

        var schemaPath = Paths.get(this.schemaFilepath);
        Object jsonSchema = null;

        try {
            jsonSchema = this.objectMapper.readValue(
                schemaPath.toFile(),
                Object.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var acl = new AgentCommunicationLanguage();
        acl.setJsonSchema(jsonSchema);
        acl.setDomain("pets");

        var version = new SemanticVersion();
        version.setMajor(1);
        version.setMinor(2);
        version.setPatch(3);
        acl.setVersion(version);

        this.cache.setAcl(acl);

        logger.info("...completed.");
    }

    @Override
    public void doTests() {
        logger.info("Proceeding to register ACL with Ubiquia...");

        String json = null;
        try {
            json = this.objectMapper.writeValueAsString(this.cache.getAcl());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        try {
            var postUrl = this.flowServiceConfig.getUrl()
                + ":"
                + this.flowServiceConfig.getPort()
                + "/agent-communication-language/register/post";

            var getUrl = this.flowServiceConfig.getUrl()
                + ":"
                + this.flowServiceConfig.getPort()
                + "/agent-communication-language/query";

            var persistedAcl = this.postAndRetriever.postAndRetrieve(
                postUrl,
                getUrl,
                this.cache.getAcl());

            this.cache.setAcl(persistedAcl);

        } catch (Exception e) {
            logger.error("ERROR: ", e);
            this.testState.addFailure("Failed to register ACL with Flow Service: "
                + e.getMessage());
        }
        logger.info("...completed.");
    }
}

