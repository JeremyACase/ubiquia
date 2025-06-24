package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.ubiquia.acl.generated.dto.Dog;
import org.ubiquia.acl.generated.entity.DogEntity;

@Component
public class DogEgressDtoMapper extends AbstractEgressDtoMapper<DogEntity, Dog> {

    protected static final Logger logger = LoggerFactory.getLogger(DogEgressDtoMapper.class);

    @Override
    public String getModelType() {
        return "Dog";
    }

    @Override
    public Dog getNewDto() {
        return new Dog();
    }


    @Override
    public Logger getLogger() {
        return logger;
    }
}