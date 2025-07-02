package org.ubiquia.core.belief.state.generator.service.builder;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguage;

@Service
public class BeliefStateNameBuilder {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateNameBuilder.class);

    public String getBeliefStateNameFrom(final AgentCommunicationLanguage acl) {
        logger.debug("Building belief state name from acl... \ndomain: {} \nversion: {}",
            acl.getDomain(),
            acl.getVersion());


        var name = acl.getDomain()
            + "-belief-state-"
            + acl.getVersion()
            + ".jar";

        logger.debug("...generated name: {}", name);

        return name;
    }
}