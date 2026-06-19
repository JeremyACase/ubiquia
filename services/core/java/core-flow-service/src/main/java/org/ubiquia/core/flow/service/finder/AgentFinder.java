package org.ubiquia.core.flow.service.finder;

import jakarta.transaction.Transactional;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;

/** Finds agent entities from the repository based on deployment criteria. */
@Service
@Transactional
public class AgentFinder {

    private static final Logger logger = LoggerFactory.getLogger(AgentFinder.class);

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private AgentConfig agentConfig;

    /** Finds the agent entity currently deploying the given graph. */
    public Optional<AgentEntity> findAgentFor(final GraphDeployment graphDeployment) {
        var ubiquiaAgentRecord = this
            .agentRepository
            .findByDeployedGraphsNameAndDeployedGraphsDomainOntologyVersionMajorAndDeployedGraphsDomainOntologyVersionMinorAndDeployedGraphsDomainOntologyVersionPatchAndId(
                graphDeployment.getGraphName(),
                graphDeployment.getDomainVersion().getMajor(),
                graphDeployment.getDomainVersion().getMinor(),
                graphDeployment.getDomainVersion().getPatch(),
                this.agentConfig.getId());
        return ubiquiaAgentRecord;
    }
}
