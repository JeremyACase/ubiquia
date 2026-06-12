package org.ubiquia.core.flow.repository;

import java.util.Optional;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.model.ubiquia.entity.ComponentEntity;

public interface ComponentRepository extends AbstractEntityRepository<ComponentEntity> {

    Optional<ComponentEntity> findByNodeId(String nodeId);

    Optional<ComponentEntity> findByNameAndGraphId(String name, String graphId);
}
