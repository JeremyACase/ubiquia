package org.ubiquia.core.flow.repository;

import java.util.Optional;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;

public interface AgentRepository extends AbstractEntityRepository<AgentEntity> {

    Optional<AgentEntity> findByGraphGraphNameAndAgentNameAndGraphVersionMajorAndGraphVersionMinorAndGraphVersionPatch(
        final String graphName,
        final String agentName,
        final Integer major,
        final Integer minor,
        final Integer patch);
}
