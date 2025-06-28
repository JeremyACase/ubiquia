package org.ubiquia.core.flow.repository;


import java.util.Optional;
import org.ubiquia.common.model.ubiquia.entity.AdapterEntity;

public interface AdapterRepository extends AbstractEntityRepository<AdapterEntity> {

    Optional<AdapterEntity> findByGraphGraphNameAndAdapterName(
        final String graphName,
        final String adapterName);
}
