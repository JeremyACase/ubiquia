package org.ubiquia.core.flow.service.factory;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

/**
 * Because it wouldn't be Java without at least one factory.
 */
@Service
@Configuration
public class AdapterFactory {

    private static final Logger logger = LoggerFactory.getLogger(AdapterFactory.class);
    @Autowired
    private AdapterBuilder adapterBuilder;
    @Autowired
    private AdapterDependencyInjector adapterDependencyInjector;

    /**
     * Make an adapter provided data from the database to do so with.
     *
     * @param adapterEntity The adapter data from the database.
     * @param graphEntity   The graph data from the database.
     * @return An adapter.
     * @throws Exception Exception from trying to build the adapter.
     */
    public AAdapter makeAdapterFor(
        final Adapter adapterEntity,
        final Graph graphEntity,
        final GraphDeployment graphDeployment)
        throws Exception {

        logger.info("...building an adapter named {} for graph {}...",
            adapterEntity.getAdapterName(),
            graphEntity.getGraphName());

        var adapter = this.makeAdapterByType(adapterEntity.getAdapterType());
        this.adapterDependencyInjector.injectFieldsFor(adapter);
        this.adapterBuilder.buildAdapter(adapter, adapterEntity, graphEntity, graphDeployment);
        adapter.initialize();

        logger.info("...completed building adapter {} for graph {}...",
            adapterEntity.getAdapterName(),
            adapterEntity.getGraph().getGraphName());
        return adapter;
    }

    /**
     * Make an adapter for a data transform.
     *
     * @param dataTransformEntity The data transform to make an adapter for.
     * @param graphDeployment     The graph deployment requesting the adapter.
     * @return An adapter.
     * @throws Exception Exceptions from building the adapter.
     */
    public AAdapter makeAdapterFor(
        final DataTransform dataTransformEntity,
        final GraphDeployment graphDeployment)
        throws Exception {

        logger.info("...building an adapter for graph {} and data transform {}...",
            dataTransformEntity.getGraph().getGraphName(),
            dataTransformEntity.getDataTransformName());

        if (Objects.isNull(dataTransformEntity.getAdapter())) {
            throw new Exception("ERROR: Cannot build an adapter from a transform with "
                + "a null adapter!");
        }

        var adapter = this.makeAdapterByType(
            dataTransformEntity.getAdapter().getAdapterType());
        this.adapterDependencyInjector.injectFieldsFor(adapter);
        this.adapterBuilder.buildAdapter(
            adapter,
            dataTransformEntity.getAdapter(),
            dataTransformEntity.getGraph(),
            graphDeployment);
        adapter.initialize();

        logger.info("...completed building adapter for graph {} and data transform named {}...",
            dataTransformEntity.getGraph().getGraphName(),
            dataTransformEntity.getDataTransformName());

        return adapter;
    }

    /**
     * Make an adapter provided a type.
     *
     * @param adapterType The type of adapter to make.
     * @return A built adapter.
     * @throws Exception Exceptions from building the new adapter.
     */
    private AAdapter makeAdapterByType(final AdapterType adapterType)
        throws Exception {
        AAdapter adapter = null;
        switch (adapterType) {

            case EGRESS: {
                adapter = new EgressAdapter();
            }
            break;

            case HIDDEN: {
                adapter = new HiddenAdapter();
            }
            break;

            case MERGE: {
                adapter = new MergeAdapter();
            }
            break;

            case POLL: {
                adapter = new PollAdapter();
            }
            break;

            case PUBLISH: {
                adapter = new PublishAdapter();
            }
            break;

            case PUSH: {
                adapter = new PushAdapter();
            }
            break;

            case QUEUE: {
                adapter = new QueueAdapter();
            }
            break;

            case SUBSCRIBE: {
                adapter = new SubscribeAdapter();
            }
            break;

            default: {
                throw new Exception("ERROR: Unrecognized adapter type: " + adapterType);
            }
        }
        return adapter;
    }
}
