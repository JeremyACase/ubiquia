package org.ubiquia.core.flow.repository;

import java.util.Optional;
import org.ubiquia.common.model.ubiquia.entity.Agent;

public interface AgentRepository extends AbstractEntityRepository<Agent> {

    Optional<Agent> findByGraphGraphNameAndAgentNameAndGraphVersionMajorAndGraphVersionMinorAndGraphVersionPatch(
        final String graphName,
        final String agentName,
        final Integer major,
        final Integer minor,
        final Integer patch);
}
