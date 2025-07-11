package org.ubiquia.core.flow.repository;

import java.util.Optional;
import org.ubiquia.common.model.ubiquia.entity.ComponentEntity;

public interface ComponentRepository extends AbstractEntityRepository<ComponentEntity> {

    Optional<ComponentEntity> findByGraphNameAndNameAndGraphVersionMajorAndGraphVersionMinorAndGraphVersionPatch(
        final String graphName,
        final String componentName,
        final Integer major,
        final Integer minor,
        final Integer patch);
}
