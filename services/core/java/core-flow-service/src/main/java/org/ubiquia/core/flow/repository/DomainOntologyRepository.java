package org.ubiquia.core.flow.repository;

import java.util.Optional;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.model.ubiquia.entity.DomainOntologyEntity;

/** Spring Data repository for domain ontology entities. */
public interface DomainOntologyRepository
    extends AbstractEntityRepository<DomainOntologyEntity> {

    /** Finds a domain ontology by its name. */
    Optional<DomainOntologyEntity> findByName(String name);
}
