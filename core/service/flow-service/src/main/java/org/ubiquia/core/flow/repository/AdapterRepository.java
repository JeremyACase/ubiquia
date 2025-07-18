package org.ubiquia.core.flow.repository;


import java.util.Optional;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.model.ubiquia.entity.AdapterEntity;

public interface AdapterRepository extends AbstractEntityRepository<AdapterEntity> {

    Optional<AdapterEntity> findByGraphNameAndName(
        final String graphName,
        final String adapterName);
}
