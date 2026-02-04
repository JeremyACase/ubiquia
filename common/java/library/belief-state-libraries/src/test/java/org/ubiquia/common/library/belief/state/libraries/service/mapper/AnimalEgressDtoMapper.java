package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.ubiquia.domain.generated.Animal;
import org.ubiquia.domain.generated.AnimalEntity;

@Component
public class AnimalEgressDtoMapper extends AbstractEgressDtoMapper<AnimalEntity, Animal> {

    protected static final Logger logger = LoggerFactory.getLogger(AnimalEgressDtoMapper.class);

    @Override
    public String getModelType() {
        return "Animal";
    }

    @Override
    public Animal getNewDto() {
        return new Animal();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}