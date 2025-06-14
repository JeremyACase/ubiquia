package org.ubiquia.core.flow.repository;


import java.util.Optional;
import org.ubiquia.common.model.ubiquia.entity.Graph;

public interface GraphRepository
    extends AbstractEntityRepository<Graph> {

    Optional<Graph> findByGraphNameAndVersionMajorAndVersionMinorAndVersionPatch(
        final String graphName,
        final Integer major,
        final Integer minor,
        final Integer patch);

    Optional<Graph> findByGraphNameAndVersionMajorAndVersionMinorAndVersionPatchAndUbiquiaAgentsDeployingGraphId(
        final String graphName,
        final Integer major,
        final Integer minor,
        final Integer patch,
        final String id);
}
