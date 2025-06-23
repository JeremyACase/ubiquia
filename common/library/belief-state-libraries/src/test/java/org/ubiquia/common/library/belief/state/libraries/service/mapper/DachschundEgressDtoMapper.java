package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.ubiquia.acl.generated.dto.Dachschund;
import org.ubiquia.acl.generated.entity.DachschundModel;

@Component
public class DachschundEgressDtoMapper extends AbstractEgressDtoMapper<DachschundModel, Dachschund> {

    protected static final Logger logger = LoggerFactory.getLogger(DachschundEgressDtoMapper.class);

    @Override
    public String getModelType() {
        return "Dachshund";
    }

    @Override
    public Dachschund getNewDto() {
        return new Dachschund();
    }


    @Override
    public Logger getLogger() {
        return logger;
    }
}