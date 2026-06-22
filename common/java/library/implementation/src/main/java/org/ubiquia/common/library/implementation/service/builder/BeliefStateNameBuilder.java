package org.ubiquia.common.library.implementation.service.builder;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.DomainDataContract;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.common.model.ubiquia.embeddable.BeliefStateGeneration;

/** Service for building belief state names in both Kubernetes and JAR formats. */
@Service
public class BeliefStateNameBuilder {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateNameBuilder.class);

    /**
     * Build a Kubernetes-compatible belief state name from a {@link BeliefStateGeneration}.
     *
     * @param generation The generation to derive the name from.
     * @return A kebab-cased Kubernetes resource name.
     */
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

    /**
     * Build a Kubernetes-compatible belief state name from a {@link DomainOntology}.
     *
     * @param domainOntology The ontology to derive the name from.
     * @return A kebab-cased Kubernetes resource name.
     */
    public String getKubernetesBeliefStateNameFrom(final DomainOntology domainOntology) {
        logger.debug("Building k8s belief state name from domain... \ndomain: {} \nversion: {}",
            domainOntology.getName(),
            domainOntology.getVersion());

        var name = domainOntology.getName()
            + "-belief-state-"
            + domainOntology.getVersion().toString().replace(".", "-");

        logger.debug("...generated name: {}", name);

        return name;
    }

    /**
     * Build a JAR file name for a belief state from a {@link DomainOntology}.
     *
     * @param domainOntology The ontology to derive the name from.
     * @return A versioned JAR filename.
     */
    public String getJarBeliefStateNameFrom(final DomainOntology domainOntology) {
        logger.debug("Building jar belief state name from acl... \ndomain: {} \nversion: {}",
            domainOntology.getName(),
            domainOntology.getVersion());

        var name = domainOntology.getName()
            + "-"
            + domainOntology.getVersion().toString()
            + ".jar";

        logger.debug("...generated name: {}", name);

        return name;
    }
}