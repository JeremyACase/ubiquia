package org.ubiquia.core.flow.service.decorator.override;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.model.dto.AdapterDto;

@Service
public class AdapterOverrideDecorator extends GenericOverrideDecorator<AdapterDto> {

    private static final Logger logger = LoggerFactory.getLogger(AdapterOverrideDecorator.class);

    @Override
    public Logger getLogger() {
        return logger;
    }

}
