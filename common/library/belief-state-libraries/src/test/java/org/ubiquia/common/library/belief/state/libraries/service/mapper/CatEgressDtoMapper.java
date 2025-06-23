package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.ubiquia.acl.generated.dto.Cat;
import org.ubiquia.acl.generated.entity.CatModel;

@Component
public class CatEgressDtoMapper extends AbstractEgressDtoMapper<CatModel, Cat> {

    protected static final Logger logger = LoggerFactory.getLogger(CatEgressDtoMapper.class);

    @Override
    public String getModelType() {
        return "Cat";
    }

    @Override
    public Cat getNewDto() {
        return new Cat();
    }


    @Override
    public Logger getLogger() {
        return logger;
    }
}