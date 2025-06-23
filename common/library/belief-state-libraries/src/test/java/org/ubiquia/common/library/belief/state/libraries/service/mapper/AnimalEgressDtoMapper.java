package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.ubiquia.acl.generated.dto.Animal;
import org.ubiquia.acl.generated.entity.AnimalModel;

@Component
public class AnimalEgressDtoMapper extends AbstractEgressDtoMapper<AnimalModel, Animal> {

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