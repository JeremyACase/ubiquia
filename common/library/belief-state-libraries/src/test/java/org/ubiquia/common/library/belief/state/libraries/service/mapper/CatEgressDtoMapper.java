package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.ubiquia.acl.generated.dto.CatDto;
import org.ubiquia.acl.generated.entity.Cat;

@Component
public class CatEgressDtoMapper extends AbstractEgressDtoMapper<Cat, CatDto> {

    protected static final Logger logger = LoggerFactory.getLogger(CatEgressDtoMapper.class);

    @Override
    public String getModelType() {
        return "Cat";
    }

    @Override
    public CatDto getNewDto() {
        return new CatDto();
    }


    @Override
    public Logger getLogger() {
        return logger;
    }
}