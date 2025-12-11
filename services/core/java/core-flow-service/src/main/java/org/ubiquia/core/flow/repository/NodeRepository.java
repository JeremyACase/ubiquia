package org.ubiquia.core.flow.repository;


import java.util.Optional;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.model.ubiquia.entity.NodeEntity;

public interface NodeRepository extends AbstractEntityRepository<NodeEntity> {

    Optional<NodeEntity> findByGraphNameAndName(
        final String graphName,
        final String nodeName);
}
