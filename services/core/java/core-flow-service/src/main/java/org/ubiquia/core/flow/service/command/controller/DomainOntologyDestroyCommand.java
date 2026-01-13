package org.ubiquia.core.flow.service.command.controller;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.model.ubiquia.entity.DomainOntologyEntity;
import org.ubiquia.core.flow.repository.*;

@Service
public class DomainOntologyDestroyCommand implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(DomainOntologyDestroyCommand.class);

    @Autowired
    private DomainOntologyRepository domainOntologyRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private DomainDataContractRepository domainDataContractRepository;

    @Autowired
    private GraphRepository graphRepository;

    public Logger getLogger() {
        return logger;
    }

    @Transactional
    public void delete(DomainOntologyEntity domainOntology) {

        logger.info("...deleting domain ontology named {} with version {}...",
            domainOntology.getName(),
            domainOntology.getVersion().toString());

        for (var graph : domainOntology.getGraphs()) {

            this.nodeRepository.deleteAll(graph.getNodes());
            this.componentRepository.deleteAll(graph.getComponents());
        }

        this.graphRepository.deleteAll(domainOntology.getGraphs());
        this.domainDataContractRepository.delete(domainOntology.getDomainDataContract());
        this.domainOntologyRepository.delete(domainOntology);

        logger.info("...deleted domain ontology named {} with version {}.",
            domainOntology.getName(),
            domainOntology.getVersion().toString());
    }
}