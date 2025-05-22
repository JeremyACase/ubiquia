package org.ubiquia.core.flow.service.decorator.override;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.models.dto.AgentDto;

@Service
public class AgentOverrideDecorator extends GenericOverrideDecorator<AgentDto> {

    private static final Logger logger = LoggerFactory.getLogger(AgentOverrideDecorator.class);

    @Override
    public Logger getLogger() {
        return logger;
    }

}
