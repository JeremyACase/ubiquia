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
import org.ubiquia.common.models.dto.AdapterDto;
import org.ubiquia.common.models.dto.GraphDto;
import org.ubiquia.common.models.embeddable.AdapterSettings;
import org.ubiquia.common.models.embeddable.EgressSettings;
import org.ubiquia.common.models.entity.Adapter;
import org.ubiquia.common.models.entity.Graph;
import org.ubiquia.core.flow.repository.AdapterRepository;
import org.ubiquia.core.flow.repository.AgentRepository;
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
    private AgentRepository agentRepository;

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
    public List<Adapter> registerAdaptersFor(
        Graph graphEntity,
        final GraphDto graphRegistration)
        throws JsonProcessingException {

        var adapterEntities = new ArrayList<Adapter>();

        var adaptersAttachedToAgents =
            this.tryRegisterAdaptersAttachedToAgents(
                graphEntity,
                graphRegistration);

        var adaptersWithoutAgents =
            this.tryRegisterAdaptersWithoutAgents(
                graphEntity,
                graphRegistration);

        adapterEntities.addAll(adaptersAttachedToAgents);
        adapterEntities.addAll(adaptersWithoutAgents);

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
        Adapter adapterEntity,
        final AdapterDto adapterRegistration) {

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

            if (Objects.nonNull(registrationSettings.getIsPersistOutputPayload())) {
                entitySettings.setIsPersistOutputPayload(registrationSettings
                    .getIsPersistOutputPayload());
            }

            if (Objects.nonNull(registrationSettings.getIsPersistInputPayload())) {
                entitySettings.setIsPersistInputPayload(registrationSettings
                    .getIsPersistInputPayload());
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
    private void tryConnectGraphAdapters(final GraphDto graphRegistration) {

        if (Objects.nonNull(graphRegistration.getEdges())) {
            for (var edge : graphRegistration.getEdges()) {
                var leftAdapterRecord = this
                    .adapterRepository
                    .findByGraphGraphNameAndAdapterName(
                        graphRegistration.getGraphName(),
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
                        .findByGraphGraphNameAndAdapterName(
                            graphRegistration.getGraphName(),
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
    private Adapter tryGetAdapterEntityFrom(
        final Graph graphEntity,
        final AdapterDto adapterRegistration) throws JsonProcessingException {

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
    private Adapter tryGetAdapterEntityHelper(
        final Graph graphEntity,
        final AdapterDto adapterRegistration) throws JsonProcessingException {

        var adapterEntity = new Adapter();
        adapterEntity.setGraph(graphEntity);
        adapterEntity.setAdapterType(adapterRegistration.getAdapterType());
        adapterEntity.setAdapterName(adapterRegistration.getAdapterName());
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

        adapterEntity.setOutputSubSchema(adapterRegistration.getOutputSubSchema());

        return adapterEntity;
    }

    /**
     * Attempt to register any adapters for a graph that are explicitly attached to agents.
     *
     * @param graphEntity       The graph to register adapters for.
     * @param graphRegistration The object representing the graph registration.
     * @return A list of newly-registered adapters.
     */
    private List<Adapter> tryRegisterAdaptersAttachedToAgents(
        Graph graphEntity,
        final GraphDto graphRegistration)
        throws JsonProcessingException {
        logger.info("...registering adapters attached to agents...");

        var adapterEntities = new ArrayList<Adapter>();

        for (var agent : graphRegistration.getAgents()) {
            logger.info("...registering adapter for agent {}...",
                agent.getAgentName());
            if (Objects.nonNull(agent.getAdapter())) {

                var agentMatch = graphEntity.getAgents()
                    .stream()
                    .filter(x -> x
                        .getAgentName()
                        .equals(agent.getAgentName()))
                    .findFirst();

                if (agentMatch.isEmpty()) {
                    throw new IllegalArgumentException("ERROR: Could not find a registered "
                        + "agent with name "
                        + agent.getAgentName());
                }

                var agentEntity = agentMatch.get();
                var adapterEntity = this.tryGetAdapterEntityFrom(
                    graphEntity,
                    agent.getAdapter());
                adapterEntity.setAgent(agentEntity);
                adapterEntity = this.adapterRepository.save(adapterEntity);
                agentEntity.setAdapter(adapterEntity);
                this.agentRepository.save(agentEntity);
                logger.info("...registered adapter for agent {}...",
                    agent.getAgentName());
                adapterEntities.add(adapterEntity);
            }
        }
        return adapterEntities;
    }

    /**
     * Attempt to register adapters for a graph that are not attached to agents.
     *
     * @param graphEntity       The graph to register adapters for.
     * @param graphRegistration The object representing the graph registration.
     * @return A list of newly-registered adapters.
     */
    private List<Adapter> tryRegisterAdaptersWithoutAgents(
        final Graph graphEntity,
        final GraphDto graphRegistration) throws JsonProcessingException {
        logger.info("...registering adapters without agents...");

        var adapterEntities = new ArrayList<Adapter>();

        for (var adapterRegistration : graphRegistration.getAgentlessAdapters()) {
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
