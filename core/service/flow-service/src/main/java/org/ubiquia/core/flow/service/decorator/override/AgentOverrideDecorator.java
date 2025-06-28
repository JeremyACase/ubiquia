package org.ubiquia.core.flow.service.decorator.override;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Agent;

@Service
public class AgentOverrideDecorator extends GenericOverrideDecorator<Agent> {

    private static final Logger logger = LoggerFactory.getLogger(AgentOverrideDecorator.class);

    @Override
    public Logger getLogger() {
        return logger;
    }

}
