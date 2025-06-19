package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.ubiquia.acl.generated.dto.AnimalDto;
import org.ubiquia.acl.generated.entity.Animal;

@Component
public class AnimalEgressDtoMapper extends AbstractEgressDtoMapper<Animal, AnimalDto> {

    protected static final Logger logger = LoggerFactory.getLogger(AnimalEgressDtoMapper.class);

    @Override
    public String getModelType() {
        return "Animal";
    }

    @Override
    public AnimalDto getNewDto() {
        return new AnimalDto();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}