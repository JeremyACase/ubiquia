package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.ubiquia.domain.generated.Person;
import org.ubiquia.domain.generated.PersonEntity;

@Component
public class PersonEgressDtoMapper extends AbstractEgressDtoMapper<PersonEntity, Person> {

    protected static final Logger logger = LoggerFactory.getLogger(PersonEgressDtoMapper.class);

    @Override
    public String getModelType() {
        return "Person";
    }

    @Override
    public Person getNewDto() {
        return new Person();
    }


    @Override
    public Logger getLogger() {
        return logger;
    }
}