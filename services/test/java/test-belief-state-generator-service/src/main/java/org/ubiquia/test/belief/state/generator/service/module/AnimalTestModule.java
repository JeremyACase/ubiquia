package org.ubiquia.test.belief.state.generator.service.module;

import static org.instancio.Select.field;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.instancio.Instancio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.domain.dto.AbstractDomainModel;
import org.ubiquia.common.test.helm.component.GenericDomainPostAndRetriever;
import org.ubiquia.common.test.helm.service.AbstractHelmTestModule;
import org.ubiquia.test.belief.state.generator.model.Animal;
import org.ubiquia.test.belief.state.generator.model.Name;
import org.ubiquia.test.belief.state.generator.service.Cache;

@Service
public class AnimalTestModule extends AbstractHelmTestModule {

    private static final Logger logger = LoggerFactory.getLogger(AnimalTestModule.class);

    @Autowired
    private Cache cache;

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GenericDomainPostAndRetriever<Animal> postAndRetriever;

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
            .ignore(field(AbstractDomainModel::getUbiquiaId))
            .ignore(field(AbstractDomainModel::getUbiquiaCreatedAt))
            .ignore(field(AbstractDomainModel::getUbiquiaUpdatedAt))
            .ignore(field(Animal::getOwner))
            .set(field(Animal::getName), name)
            .create();

        try {
            this.getLogger().info("...generated model: {}",
                this.objectMapper.writeValueAsString(animal));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        this.cache.setAnimal(animal);

        logger.info("...completed.");
    }

    @Override
    public void doTests() {
        logger.info("Proceeding to POST a model with the generated belief state...");

        try {
            var postUrl = "http://"
                + this.cache.getDomainOntology().getName()
                + "-belief-state-"
                + this.cache.getDomainOntology().getVersion().toString().replace(".", "-")
                + ":8080/ubiquia/belief-state-service/Animal/add";

            var getUrl = "http://"
                + this.cache.getDomainOntology().getName()
                + "-belief-state-"
                + this.cache.getDomainOntology().getVersion().toString().replace(".", "-")
                + ":8080/ubiquia/belief-state-service/Animal/query/";

            var persistedModel = this.postAndRetriever.postAndRetrieve(
                postUrl,
                getUrl,
                this.cache.getAnimal(),
                Animal.class);

            this.cache.setAnimal(persistedModel);

        } catch (Exception e) {
            logger.error("ERROR: ", e);
            this.testState.addFailure("Failed to register Animal with Belief State: "
                + e.getMessage());
        }
        logger.info("...completed.");
    }
}

