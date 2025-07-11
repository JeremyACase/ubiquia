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
import org.ubiquia.core.flow.service.mapper.OverrideSettingsMapper;

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
     * @param graphEntity       The graph entity to register components for.
     * @param graphRegistration The object representing the graph registration.
     * @return A list of newly-registered components.
     * @throws JsonProcessingException Exceptions from parsing component configuration.
     */
    @Transactional
    public List<ComponentEntity> registerComponentsFor(
        GraphEntity graphEntity,
        final Graph graphRegistration)
        throws JsonProcessingException {

        logger.info("...registering components for graph: {}...",
            graphRegistration.getName());

        // TODO: Verify component subschema is valid
        var componentEntities = new ArrayList<ComponentEntity>();
        for (var component : graphRegistration.getComponents()) {
            var componentEntity = this.tryGetComponentEntityFrom(
                graphRegistration,
                component);
            componentEntity = this.persistComponentWithParentGraph(
                componentEntity,
                graphEntity);
            componentEntities.add(componentEntity);
        }
        logger.info("...registered components for graph: {}...",
            graphRegistration.getName());
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
            graphEntity.setComponents(new ArrayList<>());
        }
        graphEntity.getComponents().add(componentEntity);
        graphEntity = this.graphRepository.save(graphEntity);

        componentEntity.setGraph(graphEntity);
        componentEntity = this.componentRepository.save(componentEntity);

        return componentEntity;
    }

    /**
     * Attempt to get a component entity for a graph and an associated registration object.
     *
     * @param graphRegistration     The graph to get an component for.
     * @param componentRegistration The object representing the component to be
     *                              registered.
     * @return A newly-registered component.
     * @throws JsonProcessingException Exceptions from processing the registration object.
     */
    @Transactional
    private ComponentEntity tryGetComponentEntityFrom(
        final Graph graphRegistration,
        final Component componentRegistration)
        throws JsonProcessingException {

        var record = this.componentRepository
            .findByGraphNameAndNameAndGraphVersionMajorAndGraphVersionMinorAndGraphVersionPatch(
                graphRegistration.getName(),
                componentRegistration.getName(),
                graphRegistration.getVersion().getMajor(),
                graphRegistration.getVersion().getMinor(),
                graphRegistration.getVersion().getPatch()
            );

        if (record.isPresent()) {
            throw new IllegalArgumentException("ERROR: An component with name "
                + componentRegistration.getName()
                + " already exists for a graph named "
                + graphRegistration.getVersion()
                + " with version "
                + graphRegistration.getVersion()
                + "!");
        }

        var componentEntityEntity = new ComponentEntity();

        if (Objects.nonNull(componentRegistration.getConfig())) {

            var config = new Config();

            var configMap = this.objectMapper.writeValueAsString(
                componentRegistration.getConfig().getConfigMap());
            config.setConfigMap(configMap);
            config.setConfigMountPath(componentRegistration.getConfig().getConfigMountPath());
            componentEntityEntity.setConfig(config);
        }

        componentEntityEntity.setName(componentRegistration.getName());
        componentEntityEntity.setImage(componentRegistration.getImage());

        componentEntityEntity.setEnvironmentVariables(new HashSet<>());
        if (Objects.nonNull(componentRegistration.getEnvironmentVariables())) {
            componentEntityEntity.getEnvironmentVariables()
                .addAll(componentRegistration.getEnvironmentVariables());
        }

        componentEntityEntity.setTags(new HashSet<>());
        if (Objects.nonNull(componentRegistration.getTags())) {
            componentEntityEntity.getTags().addAll(componentRegistration.getTags());
        }
        componentEntityEntity.setCommunicationServiceSettings(componentRegistration.getCommunicationServiceSettings());
        componentEntityEntity.setExposeService(componentRegistration.getExposeService());
        componentEntityEntity.setInitContainer(componentRegistration.getInitContainer());
        componentEntityEntity.setLivenessProbe(componentRegistration.getLivenessProbe());
        componentEntityEntity.setPort(componentRegistration.getPort());
        componentEntityEntity.setScaleSettings(componentRegistration.getScaleSettings());
        componentEntityEntity.setType(componentRegistration.getComponentType());

        componentEntityEntity.setOverrideSettings(new HashSet<>());
        if (Objects.nonNull(componentRegistration.getOverrideSettings())) {
            var converted = this.overrideSettingsMapper.mapToStringified(
                componentRegistration.getOverrideSettings());
            componentEntityEntity.getOverrideSettings().addAll(converted);
        }

        componentEntityEntity.setVolumes(new HashSet<>());
        if (Objects.nonNull(componentRegistration.getVolumes())) {
            componentEntityEntity.getVolumes().addAll(componentRegistration.getVolumes());
        }

        this.cleanComponent(componentEntityEntity);
        return componentEntityEntity;
    }
}
