package org.ubiquia.core.flow.service.manager;


import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.repository.GraphRepository;
import org.ubiquia.core.flow.service.command.manager.AdapterManagerCommand;
import org.ubiquia.core.flow.service.factory.AdapterFactory;

/**
 * This is a service that manages adapters at runtime. It is able to deploy them or tear them down
 * as necessary.
 */
@Service
public class AdapterManager {

    private static final Logger logger = LoggerFactory.getLogger(AdapterManager.class);
    private final HashMap<String, HashMap<String, AbstractAdapter>> adapterMap;
    @Autowired
    private AdapterFactory adapterFactory;
    @Autowired
    private AdapterManagerCommand adapterManagerCommand;
    @Autowired
    private GraphRepository graphRepository;

    public AdapterManager() {
        this.adapterMap = new HashMap<>();
    }

    /**
     * Attempt to deploy adapters for a provided graph deployment.
     *
     * @param graphDeployment The graph deployment to deploy adapters for.
     * @throws Exception Exceptions from invalid data.
     */
    @Transactional
    public void tryDeployAdaptersFor(final GraphDeployment graphDeployment) throws Exception {

        logger.info("Deploying adapters for graph {}...", graphDeployment);

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

        this.tryDeployAdaptersAttachedToAgentsFor(graphEntity, graphDeployment);
        this.tryDeployAgentlessAdaptersFor(graphEntity, graphDeployment);

        logger.info("...completed deploying adapters for graph named {}.",
            graphEntity.getGraphName());
    }

    /**
     * Attempt to tear down all currently-deployed adapters.
     */
    @Transactional
    public void teardownAllAdapters() {
        logger.info("Tearing down all deployed adapters...");
        for (var graphName : this.adapterMap.keySet()) {
            for (var adapterId : this.adapterMap.get(graphName).keySet()) {
                var adapter = this.adapterMap.get(graphName).get(adapterId);
                this.adapterManagerCommand.tearDown(adapter);
            }
        }
        this.adapterMap.clear();
        logger.info("...all adapters torn down.");
    }

    /**
     * Attempt to tear down any deployed adapters for a graph.
     *
     * @param graphName The graph name to tear down.
     * @param version   The version of the graph to tear down.
     */
    @Transactional
    public void tearDownAdaptersFor(final String graphName, final SemanticVersion version) {

        logger.info("Tearing down adapters for graph named {}...", graphName);
        var graphRecord = this.graphRepository
            .findByGraphNameAndVersionMajorAndVersionMinorAndVersionPatch(
                graphName,
                version.getMajor(),
                version.getMinor(),
                version.getPatch());
        if (graphRecord.isPresent()) {
            if (this.adapterMap.containsKey(graphName)) {
                var graphEntity = graphRecord.get();
                for (var adapterEntity : graphEntity.getAdapters()) {
                    var adapter = this.adapterMap
                        .get(graphEntity.getGraphName())
                        .get(adapterEntity.getId());

                    this.adapterMap
                        .get(graphEntity.getGraphName())
                        .remove(adapterEntity.getId());

                    this.adapterManagerCommand.tearDown(adapter);
                }
                logger.info("...completed tearing down adapters for graph named {}.",
                    graphName);
            } else {
                logger.info("...no adapters present for graph: {}... ", graphName);
            }
        } else {
            throw new IllegalArgumentException("ERROR: Could not tear down adapters; "
                + "graph doesn't exist with name "
                + graphName);
        }
    }

    /**
     * A method that will deploy adapters for a graph that are attached to agents.
     *
     * @param graphEntity The graph to deploy adapters for.
     * @throws Exception Any exceptions from attempting to deploy adapters.
     */
    private void tryDeployAdaptersAttachedToAgentsFor(
        final GraphEntity graphEntity,
        final GraphDeployment graphDeployment)
        throws Exception {
        logger.info("...deploying adapters attached to agents...");
        var graphName = graphEntity.getGraphName();
        for (var agentEntity : graphEntity.getAgents()) {
            if (Objects.nonNull(agentEntity.getAdapter())) {
                if (this.adapterMap.containsKey(graphName)
                    && this.adapterMap.get(graphName).containsKey(
                    agentEntity.getAdapter().getId())) {
                    logger.warn("WARNING: Adapter for graph {} and agent {} is "
                            + "already deployed; not deploying another adapter...",
                        graphName,
                        agentEntity.getAgentName());
                } else {
                    var adapter = this.adapterFactory.makeAdapterFor(agentEntity, graphDeployment);
                    if (!this.adapterMap.containsKey(graphName)) {
                        this.adapterMap.put(graphName, new HashMap<>());
                    }
                    this.adapterMap
                        .get(graphName)
                        .put(adapter.getAdapterContext().getAdapterId(), adapter);
                }
            } else {
                logger.info("...agent {} is a disjointed agent; "
                        + "not deploying an adapter...",
                    agentEntity.getAgentName());
            }
        }
    }

    /**
     * Attempt to deploy adapters for a graph that are not attached to data transforms.
     *
     * @param graphEntity The graph to deploy adapters for.
     * @throws Exception Exceptions from attempting to deploy the adapters.
     */
    private void tryDeployAgentlessAdaptersFor(
        final GraphEntity graphEntity,
        final GraphDeployment graphDeployment)
        throws Exception {
        logger.info("...deploying adapters that are not attached to agents...");

        for (var adapterEntity : graphEntity.getAdapters()) {

            if (Objects.isNull(adapterEntity.getAgent())) {
                if (this.adapterMap.containsKey(graphEntity.getGraphName())
                    && this.adapterMap.get(graphEntity.getGraphName())
                    .containsKey(adapterEntity.getId())) {
                    logger.warn("WARNING: {} for graph {} is "
                            + "already deployed; not deploying another adapter...",
                        adapterEntity.getAdapterName(),
                        graphEntity.getGraphName());
                } else {

                    var adapter = this.adapterFactory.makeAdapterFor(
                        adapterEntity,
                        graphEntity,
                        graphDeployment);

                    if (!this.adapterMap.containsKey(graphEntity.getGraphName())) {
                        this.adapterMap.put(graphEntity.getGraphName(), new HashMap<>());
                    }

                    this.adapterMap
                        .get(graphEntity.getGraphName())
                        .put(adapter.getAdapterContext().getAdapterId(), adapter);
                }
            }
        }
    }
}
