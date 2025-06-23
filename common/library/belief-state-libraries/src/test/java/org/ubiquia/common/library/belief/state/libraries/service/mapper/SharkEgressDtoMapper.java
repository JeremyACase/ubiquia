package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.ubiquia.acl.generated.dto.Shark;
import org.ubiquia.acl.generated.entity.SharkModel;

@Component
public class SharkEgressDtoMapper extends AbstractEgressDtoMapper<SharkModel, Shark> {

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