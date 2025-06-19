package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.ubiquia.acl.generated.dto.DachschundDto;
import org.ubiquia.acl.generated.entity.Dachschund;

@Component
public class DachschundEgressDtoMapper extends AbstractEgressDtoMapper<Dachschund, DachschundDto> {

    protected static final Logger logger = LoggerFactory.getLogger(DachschundEgressDtoMapper.class);

    @Override
    public String getModelType() {
        return "Dachshund";
    }

    @Override
    public DachschundDto getNewDto() {
        return new DachschundDto();
    }


    @Override
    public Logger getLogger() {
        return logger;
    }
}