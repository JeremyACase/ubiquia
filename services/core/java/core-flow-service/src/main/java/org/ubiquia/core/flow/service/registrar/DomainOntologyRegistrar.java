package org.ubiquia.core.flow.service.registrar;


import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.common.library.implementation.service.mapper.DomainOntologyDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;
import org.ubiquia.common.model.ubiquia.entity.DomainOntologyEntity;
import org.ubiquia.common.model.ubiquia.entity.UpdateEntity;
import org.ubiquia.core.flow.repository.DomainOntologyRepository;
import org.ubiquia.core.flow.repository.UpdateRepository;

/**
 * A service dedicated to registering components in Ubiquia.
 */
@Service
public class DomainOntologyRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(DomainOntologyRegistrar.class);

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private DomainOntologyDtoMapper domainOntologyDtoMapper;

    @Autowired
    private DomainDataContractRegistrar domainDataContractRegistrar;

    @Autowired
    private DomainOntologyRepository domainOntologyRepository;

    @Autowired
    private UpdateRepository updateRepository;

    @Autowired
    private GraphRegistrar graphRegistrar;

    @Transactional
    public DomainOntology tryRegister(final DomainOntology domainOntology)
        throws Exception {

        logger.info("Attempting to register domain ontology: {}",
            domainOntology.getName());

        var existing = this.domainOntologyRepository.findByName(domainOntology.getName());
        if (existing.isPresent()) {
            logger.info("Domain ontology '{}' already registered; skipping.",
                domainOntology.getName());
            return this.domainOntologyDtoMapper.map(existing.get());
        }

        var domainOntologyEntity = this.getEntityFrom(domainOntology);
        domainOntologyEntity = this.domainOntologyRepository.save(domainOntologyEntity);

        this
            .domainDataContractRegistrar
            .tryRegisterDomainDataContract(domainOntologyEntity, domainOntology);

        var graphEntities = this
            .graphRegistrar
            .tryRegisterGraphs(domainOntologyEntity, domainOntology);

        domainOntologyEntity.getGraphs().addAll(graphEntities);

        this.setUpdateEntity(domainOntologyEntity);
        domainOntologyEntity = this.domainOntologyRepository.save(domainOntologyEntity);

        var domainOntologyDto = this.domainOntologyDtoMapper.map(domainOntologyEntity);

        logger.info("...successfully registered domain ontology: {}",
            domainOntologyDto.getName());

        return domainOntologyDto;
    }

    @Transactional
    private AgentEntity getAgentEntity() {
        var record = this.agentRepository.findById(this.agentConfig.getId());

        if (record.isEmpty()) {
            throw new IllegalArgumentException("ERROR: Could not find a Ubiquia agent for id"
                + this.agentConfig.getId());
        }

        return record.get();
    }

    @Transactional
    private void setUpdateEntity(final DomainOntologyEntity domainOntology) {
        var updateEntity = new UpdateEntity();

        var agentEntity = this.getAgentEntity();

        updateEntity.setAgent(agentEntity);
        updateEntity.setUpdateReason("Initial persist.");
        updateEntity.setModel(domainOntology);

        domainOntology.setUpdates(new HashSet<>());
        domainOntology.getUpdates().add(updateEntity);

        this.updateRepository.save(updateEntity);
    }

    private DomainOntologyEntity getEntityFrom(final DomainOntology domainOntology) {

        var entity = new DomainOntologyEntity();
        entity.setAuthor(domainOntology.getAuthor());
        entity.setDescription(domainOntology.getDescription());
        entity.setGraphs(new ArrayList<>());
        entity.setName(domainOntology.getName());
        entity.setVersion(domainOntology.getVersion());
        return entity;
    }
}
