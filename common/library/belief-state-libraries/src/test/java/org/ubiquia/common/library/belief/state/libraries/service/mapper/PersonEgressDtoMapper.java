package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.ubiquia.acl.generated.dto.PersonDto;
import org.ubiquia.acl.generated.entity.Person;

@Component
public class PersonEgressDtoMapper extends AbstractEgressDtoMapper<Person, PersonDto> {

    protected static final Logger logger = LoggerFactory.getLogger(PersonEgressDtoMapper.class);

    @Override
    public String getModelType() {
        return "Person";
    }

    @Override
    public PersonDto getNewDto() {
        return new PersonDto();
    }


    @Override
    public Logger getLogger() {
        return logger;
    }
}