package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.ubiquia.domain.generated.Shark;
import org.ubiquia.domain.generated.SharkEntity;

@Component
public class SharkEgressDtoMapper extends AbstractEgressDtoMapper<SharkEntity, Shark> {

    protected static final Logger logger = LoggerFactory.getLogger(SharkEgressDtoMapper.class);

    @Override
    public String getModelType() {
        return "Shark";
    }

    @Override
    public Shark getNewDto() {
        return new Shark();
    }


    @Override
    public Logger getLogger() {
        return logger;
    }
}