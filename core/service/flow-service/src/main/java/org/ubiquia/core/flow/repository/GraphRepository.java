package org.ubiquia.core.flow.repository;


import java.util.Optional;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;

public interface GraphRepository
    extends AbstractEntityRepository<GraphEntity> {

    Optional<GraphEntity> findByGraphNameAndVersionMajorAndVersionMinorAndVersionPatch(
        final String graphName,
        final Integer major,
        final Integer minor,
        final Integer patch);

    Optional<GraphEntity> findByGraphNameAndVersionMajorAndVersionMinorAndVersionPatchAndUbiquiaAgentsDeployingGraphId(
        final String graphName,
        final Integer major,
        final Integer minor,
        final Integer patch,
        final String id);
}
