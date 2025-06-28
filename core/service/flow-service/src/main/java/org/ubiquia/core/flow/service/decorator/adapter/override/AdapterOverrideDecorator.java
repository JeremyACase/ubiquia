package org.ubiquia.core.flow.service.decorator.adapter.override;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Adapter;
import org.ubiquia.core.flow.service.decorator.override.GenericOverrideDecorator;

@Service
public class AdapterOverrideDecorator extends GenericOverrideDecorator<Adapter> {

    private static final Logger logger = LoggerFactory.getLogger(AdapterOverrideDecorator.class);

    @Override
    public Logger getLogger() {
        return logger;
    }

}
