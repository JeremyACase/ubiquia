package org.ubiquia.core.flow.service.decorator.override;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Component;

@Service
public class ComponentOverrideDecorator extends GenericOverrideDecorator<Component> {

    private static final Logger logger = LoggerFactory.getLogger(ComponentOverrideDecorator.class);

    @Override
    public Logger getLogger() {
        return logger;
    }

}
