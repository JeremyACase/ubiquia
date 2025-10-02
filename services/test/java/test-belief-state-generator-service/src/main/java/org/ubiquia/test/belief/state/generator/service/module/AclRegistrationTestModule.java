package org.ubiquia.test.belief.state.generator.service.module;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguage;
import org.ubiquia.common.test.helm.component.GenericUbiquiaPostAndRetriever;
import org.ubiquia.common.test.helm.service.AbstractHelmTestModule;
import org.ubiquia.test.belief.state.generator.service.Cache;

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

    @Value("${ubiquia.test.acl.filepath}")
    private String schemaFilepath;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void doSetup() {
        logger.info("Proceeding to read in an ACL...");

        var aclPath = Paths.get(this.schemaFilepath);
        AgentCommunicationLanguage acl = null;

        try {
            acl = this.objectMapper.readValue(
                aclPath.toFile(),
                AgentCommunicationLanguage.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.cache.setAcl(acl);

        logger.info("...completed.");
    }

    @Override
    public void doTests() {

        var registered = this.aclHasBeenRegistered();
        if (!registered) {
            logger.info("Proceeding to register ACL with Ubiquia...");

            try {
                var getUrl = this.flowServiceConfig.getUrl()
                    + ":"
                    + this.flowServiceConfig.getPort()
                    + "/ubiquia/flow-service/agent-communication-language/query";

                var postUrl = this.flowServiceConfig.getUrl()
                    + ":"
                    + this.flowServiceConfig.getPort()
                    + "/ubiquia/flow-service/agent-communication-language/register/post";

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
        } else {
            logger.info("...ACL has already been registered; skipping registration...");
        }
    }

    private Boolean aclHasBeenRegistered() {
        var aclIsRegistered = false;

        logger.info("...determining if ACL has already been registered...");

        var acl = this.cache.getAcl();

        var uri = UriComponentsBuilder.fromHttpUrl(
                this.flowServiceConfig.getUrl()
                    + ":"
                    + this.flowServiceConfig.getPort())
            .path("/ubiquia/flow-service/agent-communication-language/query/params")
            .queryParam("page", "0")
            .queryParam("size", "1")
            .queryParam("domain", acl.getDomain())
            .queryParam("version.major", acl.getVersion().getMajor())
            .queryParam("version.minor", acl.getVersion().getMinor())
            .queryParam("version.patch", acl.getVersion().getPatch())
            .build()
            .toUri();
        logger.info("...querying URI {}...", uri);

        var typeReference = new ParameterizedTypeReference<
            GenericPageImplementation<
                AgentCommunicationLanguage>>() {};

        var response = this.restTemplate.exchange(
            uri,
            HttpMethod.GET,
            null,
            typeReference
        );

        if (response.getBody().getTotalElements() > 0) {
            var persistedAcl = response.getBody().getContent().get(0);

            try {
                logger.info("...ACL has been registered: {}",
                    this.objectMapper.writeValueAsString(persistedAcl));
            } catch (JsonProcessingException e) {
                logger.error("ERROR: {}", e.getMessage());
                throw new RuntimeException(e);
            }
            this.cache.setAcl(persistedAcl);
            aclIsRegistered = true;
        }

        return aclIsRegistered;
    }
}

