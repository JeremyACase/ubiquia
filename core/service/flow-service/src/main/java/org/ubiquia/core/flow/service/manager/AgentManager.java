package org.ubiquia.core.flow.service.manager;


import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ubiquia.common.models.embeddable.GraphDeployment;
import org.ubiquia.common.models.enums.AgentType;
import org.ubiquia.core.flow.repository.GraphRepository;
import org.ubiquia.core.flow.service.decorator.override.AgentOverrideDecorator;
import org.ubiquia.core.flow.service.k8s.AgentOperator;
import org.ubiquia.core.flow.service.mapper.AgentDtoMapper;
import org.ubiquia.core.flow.service.mapper.GraphDtoMapper;

/**
 * This is a service that will manage agents for Ubiquia at runtime.
 */
@Component
public class AgentManager {

    private static final Logger logger = LoggerFactory.getLogger(AgentManager.class);
    @Autowired(required = false)
    private AgentOperator agentOperator;
    @Autowired
    private AgentDtoMapper agentDtoMapper;
    @Autowired
    private AgentOverrideDecorator agentOverrideDecorator;
    @Autowired
    private GraphRepository graphRepository;
    @Autowired
    private GraphDtoMapper graphDtoMapper;

    /**
     * Attempt to deploy agents for a provided graph deployment.
     *
     * @param graphDeployment The graph deployment to deploy agents for.
     * @throws JsonProcessingException Exceptions from parsing stringified objects.
     * @throws IllegalAccessException  Exceptions from trying to override values.
     */
    @Transactional
    public void tryDeployAgentsFor(final GraphDeployment graphDeployment)
        throws JsonProcessingException,
        IllegalAccessException {

        logger.info("Attempting to deploy a list of agents for graph {} with settings "
                + "{}...",
            graphDeployment.getName(),
            graphDeployment.getGraphSettings());

        var graphRecord = this
            .graphRepository
            .findByGraphNameAndVersionMajorAndVersionMinorAndVersionPatch(
                graphDeployment.getName(),
                graphDeployment.getVersion().getMajor(),
                graphDeployment.getVersion().getMinor(),
                graphDeployment.getVersion().getPatch());

        if (graphRecord.isEmpty()) {
            throw new IllegalArgumentException("ERROR: Could not find graph: " + graphDeployment);
        }
        var graphEntity = graphRecord.get();

        for (var agentEntity : graphEntity.getAgents()) {
            logger.info("...deploying agent: {}",
                agentEntity.getAgentName());

            var agent = this.agentDtoMapper.map(agentEntity);

            // Set the graph here as the DTO Mapper will not.
            agent.setGraph(this.graphDtoMapper.map(graphEntity));

            this.agentOverrideDecorator.tryOverrideBaselineValues(
                agent,
                agentEntity.getOverrideSettings().stream().toList(),
                graphDeployment);

            if (agent.getAgentType().equals(AgentType.POD)) {
                if (Objects.isNull(this.agentOperator)) {
                    throw new IllegalArgumentException("ERROR: Kubernetes is not enabled and "
                        + "the agent is a POD type!"
                        + " Cannot deploy POD agents without Kubernetes!");
                } else {
                    this.agentOperator.tryDeployAgent(agent);
                }
            } else {
                logger.info("...agent is not a POD type; not deploying...");
            }
        }
        logger.info("...completed deploying agents.");
    }
}
