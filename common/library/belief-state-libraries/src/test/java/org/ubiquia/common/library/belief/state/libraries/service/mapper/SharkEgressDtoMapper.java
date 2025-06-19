package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.ubiquia.acl.generated.dto.SharkDto;
import org.ubiquia.acl.generated.entity.Shark;

@Component
public class SharkEgressDtoMapper extends AbstractEgressDtoMapper<Shark, SharkDto> {

    protected static final Logger logger = LoggerFactory.getLogger(SharkEgressDtoMapper.class);

    @Override
    public String getModelType() {
        return "Shark";
    }

    @Override
    public SharkDto getNewDto() {
        return new SharkDto();
    }


    @Override
    public Logger getLogger() {
        return logger;
    }
}