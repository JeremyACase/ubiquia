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
import org.ubiquia.common.model.ubiquia.dto.Adapter;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.model.ubiquia.embeddable.AdapterSettings;
import org.ubiquia.common.model.ubiquia.embeddable.CommunicationServiceSettings;
import org.ubiquia.common.model.ubiquia.embeddable.EgressSettings;
import org.ubiquia.common.model.ubiquia.entity.AdapterEntity;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;
import org.ubiquia.core.flow.repository.AdapterRepository;
import org.ubiquia.core.flow.repository.ComponentRepository;
import org.ubiquia.core.flow.repository.GraphRepository;
import org.ubiquia.core.flow.service.logic.adapter.AdapterTypeLogic;
import org.ubiquia.core.flow.service.mapper.OverrideSettingsMapper;

/**
 * This is a service that can be used to register adapters with Ubiquia.
 */
@Service
public class AdapterRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(AdapterRegistrar.class);

    @Autowired
    private AdapterRepository adapterRepository;

    @Autowired
    private AdapterTypeLogic adapterTypeLogic;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private GraphRepository graphRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OverrideSettingsMapper overrideSettingsMapper;

    /**
     * Attempt to register adapters for a provided graph.
     *
     * @param graphEntity       The database entity representing the graph.
     * @param graphRegistration The registration JSON of the graph.
     * @return A list of newly-registered adapters.
     */
    @Transactional
    public List<AdapterEntity> registerAdaptersFor(
        GraphEntity graphEntity,
        final Graph graphRegistration)
        throws JsonProcessingException {

        var adapterEntities = new ArrayList<AdapterEntity>();

        var adaptersAttachedToComponents =
            this.tryRegisterAdaptersAttachedToComponents(
                graphEntity,
                graphRegistration);

        var adaptersWithoutComponents =
            this.tryRegisterAdaptersWithoutComponents(
                graphEntity,
                graphRegistration);

        adapterEntities.addAll(adaptersAttachedToComponents);
        adapterEntities.addAll(adaptersWithoutComponents);

        this.tryConnectGraphAdapters(graphRegistration);

        graphEntity.setAdapters(adapterEntities);
        this.graphRepository.save(graphEntity);

        return adapterEntities;
    }

    /**
     * A helper method to set an adapter's settings provided a registration.
     *
     * @param adapterEntity       The adapter to clean.
     * @param adapterRegistration The JSON representing the adapter's registration data.
     */
    private void setAdapterSettings(
        AdapterEntity adapterEntity,
        final Adapter adapterRegistration) {

        var entitySettings = adapterEntity.getAdapterSettings();
        var registrationSettings = adapterRegistration.getAdapterSettings();

        if (Objects.isNull(entitySettings)) {
            logger.info("...adapter has null settings; setting to default...");
            adapterEntity.setAdapterSettings(new AdapterSettings());
        } else {

            if (Objects.nonNull(registrationSettings.getInputStampKeychains())) {
                entitySettings.getInputStampKeychains().addAll(
                    registrationSettings.getInputStampKeychains());
            }

            if (Objects.nonNull(registrationSettings.getOutputStampKeychains())) {
                entitySettings.getOutputStampKeychains().addAll(
                    registrationSettings.getOutputStampKeychains());
            }

            if (Objects.nonNull(registrationSettings.getBackpressurePollFrequencyMilliseconds())) {
                entitySettings.setBackpressurePollFrequencyMilliseconds(
                    registrationSettings.getBackpressurePollFrequencyMilliseconds());
            }

            if (Objects.nonNull(registrationSettings.getInboxPollFrequencyMilliseconds())) {
                entitySettings.setInboxPollFrequencyMilliseconds(
                    registrationSettings.getInboxPollFrequencyMilliseconds());
            }

            if (Objects.nonNull(registrationSettings.getPersistOutputPayload())) {
                entitySettings.setPersistOutputPayload(registrationSettings
                    .getPersistOutputPayload());
            }

            if (Objects.nonNull(registrationSettings.getPersistInputPayload())) {
                entitySettings.setPersistInputPayload(registrationSettings
                    .getPersistInputPayload());
            }

            if (Objects.nonNull(registrationSettings.getValidateInputPayload())) {
                entitySettings.setValidateInputPayload(registrationSettings
                    .getValidateInputPayload());
            }

            if (Objects.nonNull(registrationSettings.getIsPassthrough())) {
                entitySettings.setIsPassthrough(registrationSettings
                    .getIsPassthrough());
            }

            if (Objects.nonNull(registrationSettings.getValidateOutputPayload())) {
                entitySettings.setValidateOutputPayload(registrationSettings
                    .getValidateOutputPayload());
            }
        }

        if (this.adapterTypeLogic.adapterTypeRequiresEgressSettings(
            adapterEntity.getAdapterType())) {
            if (Objects.isNull(adapterEntity.getEgressSettings())) {
                logger.info("...adapter egress settings are null but are required for {} adapter;"
                        + " setting default egress settings...",
                    adapterEntity.getAdapterType());
                adapterEntity.setEgressSettings(new EgressSettings());
            }
        }
    }

    /**
     * Provided a graph, attempt to "connect" adapters by setting their upstream/downstream
     * counterparts.
     *
     * @param graphRegistration The graph to connect adapters for.
     */
    @Transactional
    private void tryConnectGraphAdapters(final Graph graphRegistration) {

        if (Objects.nonNull(graphRegistration.getEdges())) {
            for (var edge : graphRegistration.getEdges()) {
                var leftAdapterRecord = this
                    .adapterRepository
                    .findByGraphNameAndName(
                        graphRegistration.getName(),
                        edge.getLeftAdapterName());

                if (leftAdapterRecord.isEmpty()) {
                    throw new IllegalArgumentException("ERROR: Could not find left adapter registered "
                        + "named: "
                        + edge.getLeftAdapterName());
                }
                var leftAdapter = leftAdapterRecord.get();

                for (var rightAdapterName : edge.getRightAdapterNames()) {
                    var rightAdapterRecord = this
                        .adapterRepository
                        .findByGraphNameAndName(
                            graphRegistration.getName(),
                            rightAdapterName);

                    if (rightAdapterRecord.isEmpty()) {
                        throw new IllegalArgumentException("ERROR: Could not find right adapter registered "
                            + "named: "
                            + rightAdapterName);
                    }
                    var rightAdapter = rightAdapterRecord.get();

                    leftAdapter.getDownstreamAdapters().add(rightAdapter);
                    rightAdapter.getUpstreamAdapters().add(leftAdapter);
                    this.adapterRepository.save(rightAdapter);
                }
                this.adapterRepository.save(leftAdapter);
            }
        }
    }

    /**
     * Provided a graph and an associated adapter registration, attempt to get an adapter entity.
     *
     * @param graphEntity         The graph entity to get an adapter for.
     * @param adapterRegistration The registration object representing the adapter.
     * @return An adapter entity.
     */
    @Transactional
    private AdapterEntity tryGetAdapterEntityFrom(
        final GraphEntity graphEntity,
        final Adapter adapterRegistration) throws JsonProcessingException {

        var adapterEntity = this.tryGetAdapterEntityHelper(graphEntity, adapterRegistration);
        this.setAdapterSettings(adapterEntity, adapterRegistration);
        return adapterEntity;
    }

    /**
     * A helper method that can build a new adapter entity.
     *
     * @param graphEntity         The graph entity we're building an adapter for.
     * @param adapterRegistration The object representing a new adapter.
     * @return A new adapter entity.
     */
    private AdapterEntity tryGetAdapterEntityHelper(
        final GraphEntity graphEntity,
        final Adapter adapterRegistration) throws JsonProcessingException {

        var adapterEntity = new AdapterEntity();
        adapterEntity.setGraph(graphEntity);
        adapterEntity.setCommunicationServiceSettings(adapterRegistration.getCommunicationServiceSettings());
        adapterEntity.setAdapterType(adapterRegistration.getAdapterType());
        adapterEntity.setName(adapterRegistration.getName());
        adapterEntity.setAdapterSettings(adapterRegistration.getAdapterSettings());
        adapterEntity.setBrokerSettings(adapterRegistration.getBrokerSettings());
        adapterEntity.setDescription(adapterRegistration.getDescription());
        adapterEntity.setEgressSettings(adapterRegistration.getEgressSettings());
        adapterEntity.setEndpoint(adapterRegistration.getEndpoint());
        adapterEntity.setPollSettings(adapterRegistration.getPollSettings());
        adapterEntity.setUpstreamAdapters(new ArrayList<>());
        adapterEntity.setDownstreamAdapters(new ArrayList<>());
        adapterEntity.setOutboxMessages(new ArrayList<>());

        adapterEntity.setOverrideSettings(new HashSet<>());
        if (Objects.nonNull(adapterRegistration.getOverrideSettings())) {
            var converted = this.overrideSettingsMapper.mapToStringified(
                adapterRegistration.getOverrideSettings());
            adapterEntity.getOverrideSettings().addAll(converted);
        }

        adapterEntity.setInputSubSchemas(new HashSet<>());
        if (Objects.nonNull(adapterRegistration.getInputSubSchemas())) {
            for (var schema : adapterRegistration.getInputSubSchemas()) {
                adapterEntity.getInputSubSchemas().add(schema);
            }
        }

        if (Objects.isNull(adapterEntity.getCommunicationServiceSettings())) {
            adapterEntity.setCommunicationServiceSettings(new CommunicationServiceSettings());
        }

        adapterEntity.setOutputSubSchema(adapterRegistration.getOutputSubSchema());

        return adapterEntity;
    }

    /**
     * Attempt to register any adapters for a graph that are explicitly attached to components.
     *
     * @param graphEntity       The graph to register adapters for.
     * @param graphRegistration The object representing the graph registration.
     * @return A list of newly-registered adapters.
     */
    private List<AdapterEntity> tryRegisterAdaptersAttachedToComponents(
        GraphEntity graphEntity,
        final Graph graphRegistration)
        throws JsonProcessingException {
        logger.info("...registering adapters attached to components...");

        var adapterEntities = new ArrayList<AdapterEntity>();

        for (var component : graphRegistration.getComponents()) {
            logger.info("...registering adapter for component {}...",
                component.getName());
            if (Objects.nonNull(component.getAdapter())) {

                var match = graphEntity.getComponents()
                    .stream()
                    .filter(x -> x
                        .getName()
                        .equals(component.getName()))
                    .findFirst();

                if (match.isEmpty()) {
                    throw new IllegalArgumentException("ERROR: Could not find a registered "
                        + "component with name "
                        + component.getName());
                }

                var componentEntity = match.get();
                var adapterEntity = this.tryGetAdapterEntityFrom(
                    graphEntity,
                    component.getAdapter());
                adapterEntity.setComponent(componentEntity);
                adapterEntity = this.adapterRepository.save(adapterEntity);
                componentEntity.setAdapter(adapterEntity);
                this.componentRepository.save(componentEntity);
                logger.info("...registered adapter for component {}...",
                    component.getName());
                adapterEntities.add(adapterEntity);
            }
        }
        return adapterEntities;
    }

    /**
     * Attempt to register adapters for a graph that are not attached to components.
     *
     * @param graphEntity       The graph to register adapters for.
     * @param graphRegistration The object representing the graph registration.
     * @return A list of newly-registered adapters.
     */
    private List<AdapterEntity> tryRegisterAdaptersWithoutComponents(
        final GraphEntity graphEntity,
        final Graph graphRegistration) throws JsonProcessingException {
        logger.info("...registering adapters without components...");

        var adapterEntities = new ArrayList<AdapterEntity>();

        for (var adapterRegistration : graphRegistration.getComponentlessAdapters()) {
            logger.info("...registering adapter of type {}...",
                adapterRegistration.getAdapterType());
            var adapterEntity = this.tryGetAdapterEntityFrom(
                graphEntity,
                adapterRegistration);
            adapterEntity = this.adapterRepository.save(adapterEntity);
            adapterEntities.add(adapterEntity);
        }
        return adapterEntities;
    }
}
