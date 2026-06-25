package org.ubiquia.core.flow.repository;

import java.util.List;
import java.util.Optional;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.model.ubiquia.entity.NodeEntity;

/** Spring Data repository for node entities. */
public interface NodeRepository extends AbstractEntityRepository<NodeEntity> {

    /** Finds a node by its parent graph name and node name. */
    Optional<NodeEntity> findByParentGraphNameAndName(
        final String graphName,
        final String nodeName);

    /** Finds all entry nodes (no upstream nodes) belonging to a given graph. */
    List<NodeEntity> findByParentGraphIdAndUpstreamNodesIsEmpty(String parentGraphId);
}
