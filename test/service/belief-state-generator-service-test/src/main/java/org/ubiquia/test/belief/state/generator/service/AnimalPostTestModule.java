package org.ubiquia.test.belief.state.generator.service;

import static org.instancio.Select.field;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.jar.Attributes;
import org.instancio.Instancio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.ubiquia.dto.AbstractModel;
import org.ubiquia.common.model.ubiquia.dto.Adapter;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguage;
import org.ubiquia.common.model.ubiquia.embeddable.AdapterSettings;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;
import org.ubiquia.common.test.helm.component.GenericAclPostAndRetriever;
import org.ubiquia.common.test.helm.component.GenericUbiquiaPostAndRetriever;
import org.ubiquia.common.test.helm.service.AbstractHelmTestModule;
import org.ubiquia.test.belief.state.generator.model.Animal;
import org.ubiquia.test.belief.state.generator.model.Name;

@Service
public class AnimalPostTestModule extends AbstractHelmTestModule {

    private static final Logger logger = LoggerFactory.getLogger(AnimalPostTestModule.class);

    @Autowired
    private Cache cache;

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GenericAclPostAndRetriever<Animal> postAndRetriever;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void doSetup() {
        logger.info("Proceeding to generate dummy data...");

        var name = Instancio
            .of(Name.class)
            .create();

        var animal = Instancio
            .of(Animal.class)
            .ignore(field(Animal::getOwner))
            .set(field(Animal::getName), name)
            .create();

        try {
            logger.debug("...generated model: {}",
                this.objectMapper.writeValueAsString(animal));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        this.cache.setAnimal(animal);

        logger.info("...completed.");
    }

    @Override
    public void doTests() {
        logger.info("Proceeding to POST a post with with Ubiquia belief state...");

        String json = null;
        try {
            json = this.objectMapper.writeValueAsString(this.cache.getAcl());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        try {
            var postUrl = this.cache.getAcl().getDomain()
                + "-belief-state-"
                + this.cache.getAcl().getVersion().toString().replace(".", "-")
                + ":"
                + "8080/Animal/add";

            var getUrl = this.cache.getAcl().getDomain()
                + "-belief-state-"
                + this.cache.getAcl().getVersion().toString().replace(".", "-")
                + ":"
                + "8080/query/";

            var persistedModel = this.postAndRetriever.postAndRetrieve(
                postUrl,
                getUrl,
                this.cache.getAnimal());

            this.cache.setAnimal(persistedModel);

        } catch (Exception e) {
            logger.error("ERROR: ", e);
            this.testState.addFailure("Failed to register Animal with Belief State: "
                + e.getMessage());
        }
        logger.info("...completed.");
    }
}

