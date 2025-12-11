package org.ubiquia.core.flow.service.registrar;


import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.implementation.service.mapper.DomainOntologyDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.common.model.ubiquia.entity.DomainOntologyEntity;
import org.ubiquia.core.flow.repository.DomainOntologyRepository;

/**
 * A service dedicated to registering components in Ubiquia.
 */
@Service
public class DomainOntologyRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(DomainOntologyRegistrar.class);

    @Autowired
    private DomainOntologyDtoMapper domainOntologyDtoMapper;

    @Autowired
    private DomainDataContractRegistrar domainDataContractRegistrar;

    @Autowired
    private DomainOntologyRepository domainOntologyRepository;

    @Autowired
    private GraphRegistrar graphRegistrar;

    @Transactional
    public DomainOntology tryRegister(final DomainOntology domainOntology)
        throws Exception {

        logger.info("Attempting to register domain ontology: {}",
            domainOntology.getName());

        var domainOntologyEntity = this.getEntityFrom(domainOntology);

        this
            .domainDataContractRegistrar
            .tryRegisterDomainDataContract(domainOntologyEntity, domainOntology);

        this.graphRegistrar.tryRegisterGraphs(domainOntologyEntity, domainOntology);

        var domainOntologyDto = this.domainOntologyDtoMapper.map(domainOntologyEntity);

        logger.info("...successfully registered domain ontology: {}",
            domainOntologyDto.getName());

        return domainOntologyDto;
    }

    @Transactional
    private DomainOntologyEntity getEntityFrom(final DomainOntology domainOntology) {

        var entity = new DomainOntologyEntity();
        entity.setName(domainOntology.getName());
        entity.setVersion(domainOntology.getVersion());
        entity = this.domainOntologyRepository.save(entity);

        return entity;
    }
}
