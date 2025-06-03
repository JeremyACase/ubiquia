package org.ubiquia.core.flow.repository;


import java.util.Optional;
import org.ubiquia.common.model.ubiquia.entity.Adapter;

public interface AdapterRepository extends AbstractEntityRepository<Adapter> {

    Optional<Adapter> findByGraphGraphNameAndAdapterName(
        final String graphName,
        final String adapterName);
}
