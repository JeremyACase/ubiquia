package org.ubiquia.core.flow.repository;

import java.util.Optional;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;

/** Spring Data repository for graph entities. */
public interface GraphRepository
    extends AbstractEntityRepository<GraphEntity> {

    /** Finds a graph by name, ontology name, and semantic version. */
    Optional<GraphEntity> findByNameAndDomainOntologyNameAndDomainOntologyVersionMajorAndDomainOntologyVersionMinorAndDomainOntologyVersionPatch(
        final String graphName,
        final String domainOntologyName,
        final Integer major,
        final Integer minor,
        final Integer patch);

    /** Finds a graph by name scoped to a specific domain ontology. */
    Optional<GraphEntity> findByNameAndDomainOntologyId(
        final String graphName,
        final String domainOntologyId);

    /** Finds a deployed graph by name, ontology name, semantic version, and deploying agent ID. */
    Optional<GraphEntity> findByNameAndDomainOntologyNameAndDomainOntologyVersionMajorAndDomainOntologyVersionMinorAndDomainOntologyVersionPatchAndAgentsDeployingGraphId(
        final String graphName,
        final String domainOntologyName,
        final Integer major,
        final Integer minor,
        final Integer patch,
        final String agentId);
}
