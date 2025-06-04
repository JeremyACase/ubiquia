package org.ubiquia.core.flow.service.registrar;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.hibernate.type.descriptor.java.ObjectJavaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.AgentDto;
import org.ubiquia.common.model.ubiquia.dto.GraphDto;
import org.ubiquia.common.model.ubiquia.embeddable.CommServiceSettings;
import org.ubiquia.common.model.ubiquia.embeddable.Config;
import org.ubiquia.common.model.ubiquia.embeddable.ScaleSettings;
import org.ubiquia.common.model.ubiquia.entity.Agent;
import org.ubiquia.common.model.ubiquia.entity.Graph;
import org.ubiquia.common.model.ubiquia.enums.AgentType;
import org.ubiquia.core.flow.repository.AgentRepository;
import org.ubiquia.core.flow.repository.GraphRepository;
import org.ubiquia.core.flow.service.mapper.OverrideSettingsMapper;

/**
 * A service dedicated to registering agents in Ubiquia.
 */
@Service
public class AgentRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(AgentRegistrar.class);
    @Autowired
    private AgentRepository agentRepository;
    @Autowired
    private GraphRepository graphRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OverrideSettingsMapper overrideSettingsMapper;

    /**
     * Register agents for a graph.
     *
     * @param graphEntity       The graph entity to register agents for.
     * @param graphRegistration The object representing the graph registration.
     * @return A list of newly-registered agents.
     * @throws JsonProcessingException Exceptions from parsing agent configuration.
     */
    @Transactional
    public List<Agent> registerAgentsFor(
        Graph graphEntity,
        final GraphDto graphRegistration)
        throws JsonProcessingException {

        logger.info("...registering agents for graph: {}...",
            graphRegistration.getGraphName());

        // TODO: Verify agent subschema is valid
        var agentEntities = new ArrayList<Agent>();
        for (var agent : graphRegistration.getAgents()) {
            var agentEntity = this.tryGetAgentEntityFrom(
                graphRegistration,
                agent);
            agentEntity = this.persistAgentWithParentGraph(
                agentEntity,
                graphEntity);
            agentEntities.add(agentEntity);
        }
        logger.info("...registered agents for graph: {}...",
            graphRegistration.getGraphName());
        return agentEntities;
    }

    /**
     * Clean an agent by setting fields to default values as necessary.
     *
     * @param agentEntity The agent to clean.
     */
    private void cleanAgent(Agent agentEntity) {

        if (Objects.isNull(agentEntity.getTags())) {
            agentEntity.setTags(new HashSet<>());
        }

        if (Objects.isNull(agentEntity.getType())) {
            agentEntity.setType(AgentType.NONE);
            logger.debug("...agent has null type; setting to {}...",
                agentEntity.getType());
        }

        if (Objects.isNull(agentEntity.getScaleSettings())) {
            agentEntity.setScaleSettings(new ScaleSettings());
        }

        if (Objects.isNull(agentEntity.getVolumes())) {
            agentEntity.setVolumes(new HashSet<>());
        }

        if (Objects.isNull(agentEntity.getEnvironmentVariables())) {
            agentEntity.setEnvironmentVariables(new HashSet<>());
        }

        if (Objects.isNull(agentEntity.getExposeService())) {
            agentEntity.setExposeService(false);
        }

        if (Objects.isNull(agentEntity.getCommServiceSettings())) {
            agentEntity.setCommServiceSettings(new CommServiceSettings());
        }
    }

    /**
     * Persist an agent in the database against a parent graph.
     *
     * @param agentEntity The agent entity to persist.
     * @param graphEntity The graph to be associated with the agent.
     * @return The newly-registered agent.
     */
    @Transactional
    private Agent persistAgentWithParentGraph(Agent agentEntity, Graph graphEntity) {

        if (Objects.isNull(graphEntity.getAgents())) {
            graphEntity.setAgents(new ArrayList<>());
        }
        graphEntity.getAgents().add(agentEntity);
        graphEntity = this.graphRepository.save(graphEntity);

        agentEntity.setGraph(graphEntity);
        agentEntity = this.agentRepository.save(agentEntity);

        return agentEntity;
    }

    /**
     * Attempt to get an agent entity for a graph and an associated registration object.
     *
     * @param graphRegistration The graph to get an agent for.
     * @param agentRegistration The object representing the agent to be
     *                          registered.
     * @return A newly-registered agent.
     * @throws JsonProcessingException Exceptions from processing the registration object.
     */
    @Transactional
    private Agent tryGetAgentEntityFrom(
        final GraphDto graphRegistration,
        final AgentDto agentRegistration)
        throws JsonProcessingException {

        var record = this.agentRepository
            .findByGraphGraphNameAndAgentNameAndGraphVersionMajorAndGraphVersionMinorAndGraphVersionPatch(
                graphRegistration.getGraphName(),
                agentRegistration.getAgentName(),
                graphRegistration.getVersion().getMajor(),
                graphRegistration.getVersion().getMinor(),
                graphRegistration.getVersion().getPatch()
            );

        if (record.isPresent()) {
            throw new IllegalArgumentException("ERROR: An agent with name "
                + agentRegistration.getAgentName()
                + " already exists for a graph named "
                + graphRegistration.getVersion()
                + " with version "
                + graphRegistration.getVersion()
                + "!");
        }

        var agentEntity = new Agent();

        if (Objects.nonNull(agentRegistration.getConfig())) {

            var config = new Config();

            var configMap = this.objectMapper.writeValueAsString(
                agentRegistration.getConfig().getConfigMap());
            config.setConfigMap(configMap);
            config.setConfigMountPath(agentRegistration.getConfig().getConfigMountPath());
            agentEntity.setConfig(config);
        }

        agentEntity.setAgentName(agentRegistration.getAgentName());
        agentEntity.setImage(agentRegistration.getImage());

        agentEntity.setEnvironmentVariables(new HashSet<>());
        if (Objects.nonNull(agentRegistration.getEnvironmentVariables())) {
            agentEntity.getEnvironmentVariables()
                .addAll(agentRegistration.getEnvironmentVariables());
        }

        agentEntity.setTags(new HashSet<>());
        if (Objects.nonNull(agentRegistration.getTags())) {
            agentEntity.getTags().addAll(agentRegistration.getTags());
        }
        agentEntity.setCommServiceSettings(agentRegistration.getCommServiceSettings());
        agentEntity.setExposeService(agentRegistration.getExposeService());
        agentEntity.setInitContainer(agentRegistration.getInitContainer());
        agentEntity.setLivenessProbe(agentRegistration.getLivenessProbe());
        agentEntity.setPort(agentRegistration.getPort());
        agentEntity.setScaleSettings(agentRegistration.getScaleSettings());
        agentEntity.setType(agentRegistration.getAgentType());

        agentEntity.setOverrideSettings(new HashSet<>());
        if (Objects.nonNull(agentRegistration.getOverrideSettings())) {
            var converted = this.overrideSettingsMapper.mapToStringified(
                agentRegistration.getOverrideSettings());
            agentEntity.getOverrideSettings().addAll(converted);
        }

        agentEntity.setVolumes(new HashSet<>());
        if (Objects.nonNull(agentRegistration.getVolumes())) {
            agentEntity.getVolumes().addAll(agentRegistration.getVolumes());
        }

        this.cleanAgent(agentEntity);
        return agentEntity;
    }
}
