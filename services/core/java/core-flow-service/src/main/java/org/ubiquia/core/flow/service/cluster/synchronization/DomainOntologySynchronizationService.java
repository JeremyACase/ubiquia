package org.ubiquia.core.flow.service.cluster.synchronization;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.library.implementation.service.mapper.DomainOntologyDtoMapper;
import org.ubiquia.common.library.implementation.service.mapper.GenericDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.common.model.ubiquia.entity.DomainOntologyEntity;
import org.ubiquia.core.flow.repository.DomainOntologyRepository;

@Service
public class DomainOntologySynchronizationService
    extends AbstractSynchronizationService<DomainOntologyEntity, DomainOntology> {

    private static final String ENDPOINT_PATH =
        "/ubiquia/core/flow-service/domain-ontology/register/post";

    @Autowired
    private DomainOntologyRepository domainOntologyRepository;

    @Autowired
    private DomainOntologyDtoMapper domainOntologyDtoMapper;

    @Override
    protected AbstractEntityRepository<DomainOntologyEntity> getRepository() {
        return this.domainOntologyRepository;
    }

    @Override
    protected GenericDtoMapper<DomainOntologyEntity, DomainOntology> getMapper() {
        return this.domainOntologyDtoMapper;
    }

    @Override
    protected String getEndpointPath() {
        return ENDPOINT_PATH;
    }
}
