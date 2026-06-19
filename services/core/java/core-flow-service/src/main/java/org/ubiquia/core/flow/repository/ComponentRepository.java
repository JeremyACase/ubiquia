package org.ubiquia.core.flow.repository;

import java.util.Optional;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.model.ubiquia.entity.ComponentEntity;

/** Spring Data repository for component entities. */
public interface ComponentRepository extends AbstractEntityRepository<ComponentEntity> {

    /** Finds a component by its associated node ID. */
    Optional<ComponentEntity> findByNodeId(String nodeId);

    /** Finds a component by name within a specific graph. */
    Optional<ComponentEntity> findByNameAndGraphId(String name, String graphId);
}
