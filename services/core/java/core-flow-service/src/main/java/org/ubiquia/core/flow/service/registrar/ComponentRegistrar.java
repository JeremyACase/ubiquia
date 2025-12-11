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
import org.ubiquia.common.library.implementation.service.mapper.OverrideSettingsMapper;
import org.ubiquia.common.model.ubiquia.dto.Component;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.model.ubiquia.embeddable.CommunicationServiceSettings;
import org.ubiquia.common.model.ubiquia.embeddable.Config;
import org.ubiquia.common.model.ubiquia.embeddable.ScaleSettings;
import org.ubiquia.common.model.ubiquia.entity.ComponentEntity;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;
import org.ubiquia.core.flow.repository.ComponentRepository;
import org.ubiquia.core.flow.repository.GraphRepository;

/**
 * A service dedicated to registering components in Ubiquia.
 */
@Service
public class ComponentRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(ComponentRegistrar.class);
    @Autowired
    private ComponentRepository componentRepository;
    @Autowired
    private GraphRepository graphRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OverrideSettingsMapper overrideSettingsMapper;

    /**
     * Register components for a graph.
     *
     * @param graphEntity The graph entity to register components for.
     * @param graph       The object representing the graph registration.
     * @return A list of newly-registered components.
     * @throws JsonProcessingException Exceptions from parsing component configuration.
     */
    @Transactional
    public List<ComponentEntity> registerComponentsFor(
        GraphEntity graphEntity,
        final Graph graph)
        throws JsonProcessingException {

        logger.info("...registering components for graph: {}...",
            graph.getName());

        // TODO: Verify component subschema is valid
        var componentEntities = new ArrayList<ComponentEntity>();
        for (var component : graph.getComponents()) {
            var componentEntity = this.tryGetComponentEntityFrom(
                component);
            componentEntity = this.persistComponentWithParentGraph(
                componentEntity,
                graphEntity);
            componentEntities.add(componentEntity);
        }
        logger.info("...registered components for graph: {}...",
            graph.getName());
        return componentEntities;
    }

    /**
     * Clean an component by setting fields to default values as necessary.
     *
     * @param componentEntity The component to clean.
     */
    private void cleanComponent(ComponentEntity componentEntity) {

        if (Objects.isNull(componentEntity.getTags())) {
            componentEntity.setTags(new HashSet<>());
        }

        if (Objects.isNull(componentEntity.getType())) {
            componentEntity.setType(ComponentType.NONE);
            logger.debug("...component has null type; setting to {}...",
                componentEntity.getType());
        }

        if (Objects.isNull(componentEntity.getScaleSettings())) {
            componentEntity.setScaleSettings(new ScaleSettings());
        }

        if (Objects.isNull(componentEntity.getVolumes())) {
            componentEntity.setVolumes(new HashSet<>());
        }

        if (Objects.isNull(componentEntity.getEnvironmentVariables())) {
            componentEntity.setEnvironmentVariables(new HashSet<>());
        }

        if (Objects.isNull(componentEntity.getExposeService())) {
            componentEntity.setExposeService(false);
        }

        if (Objects.isNull(componentEntity.getCommunicationServiceSettings())) {
            componentEntity.setCommunicationServiceSettings(new CommunicationServiceSettings());
        }

        if (Objects.isNull(componentEntity.getPostStartExecCommands())) {
            componentEntity.setPostStartExecCommands(new ArrayList<>());
        }
    }

    /**
     * Persist a component in the database against a parent graph.
     *
     * @param componentEntity The component entity to persist.
     * @param graphEntity     The graph to be associated with the component.
     * @return The newly-registered component.
     */
    @Transactional
    private ComponentEntity persistComponentWithParentGraph(
        ComponentEntity componentEntity,
        GraphEntity graphEntity) {

        if (Objects.isNull(graphEntity.getComponents())) {
            graphEntity.setComponents(new HashSet<>());
        }
        componentEntity.setGraph(graphEntity);
        componentEntity = this.componentRepository.save(componentEntity);

        return componentEntity;
    }

    /**
     * Attempt to get a component entity for a graph and an associated registration object.
     *
     * @param componentRegistration The object representing the component to be
     *                              registered.
     * @return A newly-registered component.
     * @throws JsonProcessingException Exceptions from processing the registration object.
     */
    @Transactional
    private ComponentEntity tryGetComponentEntityFrom(
        final Component componentRegistration)
        throws JsonProcessingException {

        var componentEntity = new ComponentEntity();

        if (Objects.nonNull(componentRegistration.getConfig())) {

            var config = new Config();

            var configMap = this.objectMapper.writeValueAsString(
                componentRegistration.getConfig().getConfigMap());
            config.setConfigMap(configMap);
            config.setConfigMountPath(componentRegistration.getConfig().getConfigMountPath());
            componentEntity.setConfig(config);
        }

        componentEntity.setName(componentRegistration.getName());
        componentEntity.setImage(componentRegistration.getImage());

        componentEntity.setEnvironmentVariables(new HashSet<>());
        if (Objects.nonNull(componentRegistration.getEnvironmentVariables())) {
            componentEntity.getEnvironmentVariables()
                .addAll(componentRegistration.getEnvironmentVariables());
        }

        componentEntity.setTags(new HashSet<>());
        if (Objects.nonNull(componentRegistration.getTags())) {
            componentEntity.getTags().addAll(componentRegistration.getTags());
        }
        componentEntity.setCommunicationServiceSettings(componentRegistration.getCommunicationServiceSettings());
        componentEntity.setExposeService(componentRegistration.getExposeService());
        componentEntity.setInitContainer(componentRegistration.getInitContainer());
        componentEntity.setLivenessProbe(componentRegistration.getLivenessProbe());
        componentEntity.setPort(componentRegistration.getPort());
        componentEntity.setScaleSettings(componentRegistration.getScaleSettings());
        componentEntity.setType(componentRegistration.getComponentType());
        componentEntity.setPostStartExecCommands(componentRegistration.getPostStartExecCommands());

        componentEntity.setOverrideSettings(new HashSet<>());
        if (Objects.nonNull(componentRegistration.getOverrideSettings())) {
            var converted = this.overrideSettingsMapper.mapToStringified(
                componentRegistration.getOverrideSettings());
            componentEntity.getOverrideSettings().addAll(converted);
        }

        componentEntity.setVolumes(new HashSet<>());
        if (Objects.nonNull(componentRegistration.getVolumes())) {
            componentEntity.getVolumes().addAll(componentRegistration.getVolumes());
        }

        this.cleanComponent(componentEntity);
        return componentEntity;
    }
}
