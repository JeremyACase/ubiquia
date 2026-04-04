package org.ubiquia.core.flow.repository;

import java.util.Optional;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.model.ubiquia.entity.DomainOntologyEntity;


public interface DomainOntologyRepository
    extends AbstractEntityRepository<DomainOntologyEntity> {

    Optional<DomainOntologyEntity> findByName(String name);
}
