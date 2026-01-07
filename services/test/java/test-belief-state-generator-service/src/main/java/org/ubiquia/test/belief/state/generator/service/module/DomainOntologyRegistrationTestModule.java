package org.ubiquia.test.belief.state.generator.service.module;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.common.test.helm.component.GenericUbiquiaPostAndRetriever;
import org.ubiquia.common.test.helm.service.AbstractHelmTestModule;
import org.ubiquia.test.belief.state.generator.service.Cache;

@Service
public class DomainOntologyRegistrationTestModule extends AbstractHelmTestModule {

    private static final Logger logger = LoggerFactory.getLogger(DomainOntologyRegistrationTestModule.class);

    @Autowired
    private Cache cache;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Autowired
    private GenericUbiquiaPostAndRetriever<DomainOntology> postAndRetriever;

    @Value("${ubiquia.test.ontology.filepath}")
    private String ontologyFilepath;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void doSetup() {
        logger.info("Proceeding to read in an domain ontology...");

        var domainOntologyFilepath = Paths.get(this.ontologyFilepath);
        DomainOntology domainOntology = null;

        var yamlMapper = new ObjectMapper(new YAMLFactory());

        try {
            domainOntology = yamlMapper
                .readValue(domainOntologyFilepath.toFile(), DomainOntology.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.cache.setDomainOntology(domainOntology);

        logger.info("...completed.");
    }

    @Override
    public void doTests() {

        var registered = this.ontologyHasBeenRegistered();
        if (!registered) {
            logger.info("Proceeding to register Ontology with Ubiquia...");

            try {
                var getUrl = this
                    .flowServiceConfig
                    .getUrl()
                    + ":"
                    + this.flowServiceConfig.getPort()
                    + "/ubiquia/core/flow-service/domain-ontology/query";

                var postUrl = this
                    .flowServiceConfig
                    .getUrl()
                    + ":"
                    + this.flowServiceConfig.getPort()
                    + "/ubiquia/core/flow-service/domain-ontology/register/post";

                var persistedOntology = this
                    .postAndRetriever
                    .postAndRetrieve(postUrl, getUrl, this.cache.getDomainOntology());

                this.cache.setDomainOntology(persistedOntology);

            } catch (Exception e) {
                logger.error("ERROR: ", e);
                this.testState.addFailure("Failed to register ontology with Flow Service: "
                    + e.getMessage());
            }
            logger.info("...completed.");
        } else {
            logger.info("...ontology has already been registered; skipping registration...");
        }
    }

    private Boolean ontologyHasBeenRegistered() {
        var ontologyIsRegistered = false;

        logger.info("...determining if ontology has already been registered...");

        var ontology = this.cache.getDomainOntology();

        var uri = UriComponentsBuilder.fromHttpUrl(this
                .flowServiceConfig
                .getUrl() + ":" + this.flowServiceConfig.getPort())
            .path("/ubiquia/core/flow-service/domain-ontology/query/params")
            .queryParam("page", "0")
            .queryParam("size", "1")
            .queryParam("name", ontology.getName())
            .queryParam("version.major", ontology.getVersion().getMajor())
            .queryParam("version.minor", ontology.getVersion().getMinor())
            .queryParam("version.patch", ontology.getVersion().getPatch())
            .build()
            .toUri();
        logger.info("...querying URI {}...", uri);

        var typeReference = new ParameterizedTypeReference<
            GenericPageImplementation<DomainOntology>>() {
        };

        var response = this
            .restTemplate
            .exchange(uri, HttpMethod.GET, null, typeReference);

        if (response.getBody().getTotalElements() > 0) {
            var persistedOntology = response.getBody().getContent().get(0);

            try {
                logger.info("...ontology has been registered: {}", this
                    .objectMapper
                    .writeValueAsString(persistedOntology));
            } catch (JsonProcessingException e) {
                logger.error("ERROR: {}", e.getMessage());
                throw new RuntimeException(e);
            }
            this.cache.setDomainOntology(persistedOntology);
            ontologyIsRegistered = true;
        }

        return ontologyIsRegistered;
    }
}

