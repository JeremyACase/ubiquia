package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.ubiquia.acl.generated.dto.DogDto;
import org.ubiquia.acl.generated.entity.Dog;

@Component
public class DogEgressDtoMapper extends AbstractEgressDtoMapper<Dog, DogDto> {

    protected static final Logger logger = LoggerFactory.getLogger(DogEgressDtoMapper.class);

    @Override
    public String getModelType() {
        return "Dog";
    }

    @Override
    public DogDto getNewDto() {
        return new DogDto();
    }


    @Override
    public Logger getLogger() {
        return logger;
    }
}