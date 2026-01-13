package org.ubiquia.core.flow.repository;


import java.util.Optional;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;

public interface GraphRepository
    extends AbstractEntityRepository<GraphEntity> {

    Optional<GraphEntity> findByNameAndDomainOntologyNameAndDomainOntologyVersionMajorAndDomainOntologyVersionMinorAndDomainOntologyVersionPatch(
        final String graphName,
        final String domainOntologyName,
        final Integer major,
        final Integer minor,
        final Integer patch);

    Optional<GraphEntity> findByDomainOntologyNameAndDomainOntologyVersionMajorAndDomainOntologyVersionMinorAndDomainOntologyVersionPatchAndAgentsDeployingGraphId(
        final String domainOntologyName,
        final Integer major,
        final Integer minor,
        final Integer patch,
        final String graphId);
}
