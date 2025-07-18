package org.ubiquia.common.library.implementation.service.builder;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguage;
import org.ubiquia.common.model.ubiquia.embeddable.BeliefStateGeneration;

@Service
public class BeliefStateNameBuilder {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateNameBuilder.class);

    public String getKubernetesBeliefStateNameFrom(final BeliefStateGeneration generation) {
        logger.debug("Building k8s belief state name from generation... \ndomain: {} \nversion: {}",
            generation.getDomainName(),
            generation.getVersion());

        var name = generation.getDomainName()
            + "-belief-state-"
            + generation.getVersion().toString().replace(".", "-");

        logger.debug("...generated name: {}", name);

        return name;
    }

    public String getKubernetesBeliefStateNameFrom(final AgentCommunicationLanguage acl) {
        logger.debug("Building k8s belief state name from acl... \ndomain: {} \nversion: {}",
            acl.getDomain(),
            acl.getVersion());

        var name = acl.getDomain()
            + "-belief-state-"
            + acl.getVersion().toString().replace(".", "-");

        logger.debug("...generated name: {}", name);

        return name;
    }

    public String getJarBeliefStateNameFrom(final AgentCommunicationLanguage acl) {
        logger.debug("Building jar belief state name from acl... \ndomain: {} \nversion: {}",
            acl.getDomain(),
            acl.getVersion());

        var name = acl.getDomain()
            + "-"
            + acl.getVersion().toString()
            + ".jar";

        logger.debug("...generated name: {}", name);

        return name;
    }
}