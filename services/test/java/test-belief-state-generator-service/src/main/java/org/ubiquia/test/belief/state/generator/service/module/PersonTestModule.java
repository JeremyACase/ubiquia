package org.ubiquia.test.belief.state.generator.service.module;

import static org.instancio.Select.field;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Objects;
import org.instancio.Instancio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.model.domain.dto.AbstractDomainModel;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.test.helm.component.GenericAclPostAndRetriever;
import org.ubiquia.common.test.helm.service.AbstractHelmTestModule;
import org.ubiquia.test.belief.state.generator.model.Name;
import org.ubiquia.test.belief.state.generator.model.Person;
import org.ubiquia.test.belief.state.generator.service.Cache;

@Service
public class PersonTestModule extends AbstractHelmTestModule {

    private static final Logger logger = LoggerFactory.getLogger(PersonTestModule.class);

    @Autowired
    private Cache cache;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GenericAclPostAndRetriever<Person> postAndRetriever;

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

        var pets = new ArrayList<>();
        pets.add(this.cache.getAnimal());

        var person = Instancio
            .of(Person.class)
            .ignore(field(AbstractDomainModel::getUbiquiaId))
            .ignore(field(AbstractDomainModel::getUbiquiaCreatedAt))
            .ignore(field(AbstractDomainModel::getUbiquiaUpdatedAt))
            .set(field(Person::getPets), pets)
            .set(field(Person::getName), name)
            .create();

        try {
            this.getLogger().info("...generated model: {}",
                this.objectMapper.writeValueAsString(person));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        this.cache.setPerson(person);

        logger.info("...completed.");
    }

    @Override
    public void doTests() {
        logger.info("Proceeding to POST a model with the generated belief state...");

        try {
            var postUrl = "http://"
                + this.cache.getAcl().getName()
                + "-belief-state-"
                + this.cache.getAcl().getVersion().toString().replace(".", "-")
                + ":8080/ubiquia/belief-state-service/Person/add";

            var getUrl = "http://"
                + this.cache.getAcl().getName()
                + "-belief-state-"
                + this.cache.getAcl().getVersion().toString().replace(".", "-")
                + ":8080/ubiquia/belief-state-service/Person/query/";

            var persistedModel = this.postAndRetriever.postAndRetrieve(
                postUrl,
                getUrl,
                this.cache.getPerson(),
                Person.class);

            this.cache.setPerson(persistedModel);

            this.tryVerifyOneToManyIsNotReturned();
            this.tryVerifyOneToManyCanBeQueried();

        } catch (Exception e) {
            logger.error("ERROR: ", e);
            this.testState.addFailure("Failed to register Person with Belief State: "
                + e.getMessage());
        }
        logger.info("...completed.");
    }

    private void tryVerifyOneToManyIsNotReturned() {
        var person = this.cache.getPerson();

        if (Objects.nonNull(person.getPets())) {
            if (person.getPets().size() > 0) {
                this.testState.addFailure("ERROR: "
                    + "Persisted person model is 1:many with pets, many side should not be "
                    + "returned on a query!");
            }
        }
    }

    private void tryVerifyOneToManyCanBeQueried() {
        var typeReference = new ParameterizedTypeReference<
            GenericPageImplementation<
                Person>>() {};

        var getUrl = "http://"
            + this.cache.getAcl().getName()
            + "-belief-state-"
            + this.cache.getAcl().getVersion().toString().replace(".", "-")
            + ":8080/ubiquia/belief-state-service/Person/query/params?page=0&size=1&pets.ubiquiaId="
            + this.cache.getAnimal().getUbiquiaId();

        logger.info("GETting from URL: {}", getUrl);

        var response = this.restTemplate.exchange(
            getUrl,
            HttpMethod.GET,
            null,
            typeReference
        );

        if (response.getStatusCode().isError()) {
            this.testState.addFailure("ERROR: "
                + response.getStatusCode());
        }

        if (response.getBody().getTotalElements() == 0) {
            this.testState.addFailure("ERROR: Could not properly query owner given a "
                + "relational pets id!");
        }
    }
}

