package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.ubiquia.acl.generated.dto.Person;
import org.ubiquia.acl.generated.entity.PersonModel;

@Component
public class PersonEgressDtoMapper extends AbstractEgressDtoMapper<PersonModel, Person> {

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