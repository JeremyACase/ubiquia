package org.ubiquia.core.flow.repository;

import java.util.Optional;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.model.ubiquia.entity.NodeEntity;

/** Spring Data repository for node entities. */
public interface NodeRepository extends AbstractEntityRepository<NodeEntity> {

    /** Finds a node by its parent graph name and node name. */
    Optional<NodeEntity> findByGraphNameAndName(
        final String graphName,
        final String nodeName);
}
