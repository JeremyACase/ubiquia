package org.ubiquia.core.flow.service.registrar;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.model.dto.AgentDto;
import org.ubiquia.core.flow.model.dto.GraphDto;
import org.ubiquia.core.flow.model.embeddable.Config;
import org.ubiquia.core.flow.model.embeddable.ScaleSettings;
import org.ubiquia.core.flow.model.entity.Agent;
import org.ubiquia.core.flow.model.entity.Graph;
import org.ubiquia.core.flow.model.enums.AgentType;
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
     * Register data transforms for a graph.
     *
     * @param graphEntity       The graph entity to register data transforms for.
     * @param graphRegistration The object representing the graph registration.
     * @return A list of newly-registered data transforms.
     * @throws JsonProcessingException Exceptions from parsing data transform configuration.
     */
    @Transactional
    public List<Agent> registerAgentsFor(
        Graph graphEntity,
        final GraphDto graphRegistration)
        throws JsonProcessingException {

        logger.info("...registering data transforms for graph: {}...",
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
        logger.info("...registered data transforms for graph: {}...",
            graphRegistration.getGraphName());
        return agentEntities;
    }

    /**
     * Clean a data transform by setting fields to default values as necessary.
     *
     * @param agentEntity The data transform to clean.
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
    }

    /**
     * Persist a data transform in the database against a parent graph.
     *
     * @param agentEntity The data transform entity to persist.
     * @param graphEntity         The graph to be associated with the data transform.
     * @return The newly-registered data transform.
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
     * Attempt to get a data transform entity for a graph and an associated registration object.
     *
     * @param graphRegistration The graph to get a data transform for.
     * @param agentRegistration The object representing the agent to be
     *                          registered.
     * @return A newly-registered data transform.
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
        agentEntity.setType(agentRegistration.getDataTransformType());
        agentEntity.setPort(agentRegistration.getPort());
        agentEntity.setInitContainer(agentRegistration.getInitContainer());
        agentEntity.setLivenessProbe(agentRegistration.getLivenessProbe());
        agentEntity.setScaleSettings(agentRegistration.getScaleSettings());

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
