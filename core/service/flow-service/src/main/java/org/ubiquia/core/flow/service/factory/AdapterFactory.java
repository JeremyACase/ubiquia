package org.ubiquia.core.flow.service.factory;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.entity.AdapterEntity;
import org.ubiquia.common.model.ubiquia.entity.ComponentEntity;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;
import org.ubiquia.common.model.ubiquia.enums.AdapterType;
import org.ubiquia.core.flow.component.adapter.*;
import org.ubiquia.core.flow.service.builder.adapter.AdapterBuilder;

/**
 * Because it wouldn't be Java without at least one factory.
 */
@Service
public class AdapterFactory {

    private static final Logger logger = LoggerFactory.getLogger(AdapterFactory.class);

    @Autowired
    private AdapterBuilder adapterBuilder;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Make an adapter provided data from the database to do so with.
     *
     * @param adapterEntity The adapter data from the database.
     * @param graphEntity   The graph data from the database.
     * @return An adapter.
     * @throws Exception Exception from trying to build the adapter.
     */
    public AbstractAdapter makeAdapterFor(
        final AdapterEntity adapterEntity,
        final GraphEntity graphEntity,
        final GraphDeployment graphDeployment)
        throws Exception {

        logger.info("...building an adapter named {} for graph {}...",
            adapterEntity.getName(),
            graphEntity.getName());

        var adapter = this.makeAdapterByType(adapterEntity.getAdapterType());
        this.adapterBuilder.buildAdapter(adapter, adapterEntity, graphEntity, graphDeployment);

        logger.info("...completed building adapter {} for graph {}...",
            adapterEntity.getName(),
            adapterEntity.getGraph().getName());
        return adapter;
    }

    /**
     * Make an adapter for a component.
     *
     * @param componentEntity The component to make an adapter for.
     * @param graphDeployment The graph deployment requesting the adapter.
     * @return An adapter.
     * @throws Exception Exceptions from building the adapter.
     */
    public AbstractAdapter makeAdapterFor(
        final ComponentEntity componentEntity,
        final GraphDeployment graphDeployment)
        throws Exception {

        logger.info("...building an adapter for graph {} and component {}...",
            componentEntity.getGraph().getName(),
            componentEntity.getName());

        if (Objects.isNull(componentEntity.getAdapter())) {
            throw new Exception("ERROR: Cannot build an adapter from an component with "
                + "a null adapter!");
        }

        var adapter = this.makeAdapterByType(componentEntity.getAdapter().getAdapterType());
        this.adapterBuilder.buildAdapter(
            adapter,
            componentEntity.getAdapter(),
            componentEntity.getGraph(),
            graphDeployment);

        logger.info("...completed building adapter for graph {} and component named {}...",
            componentEntity.getGraph().getName(),
            componentEntity.getName());

        return adapter;
    }

    /**
     * Make an adapter provided a type.
     *
     * @param adapterType The type of adapter to make.
     * @return A built adapter.
     * @throws Exception Exceptions from building the new adapter.
     */
    private AbstractAdapter makeAdapterByType(final AdapterType adapterType)
        throws Exception {
        AbstractAdapter adapter = null;

        switch (adapterType) {

            case EGRESS: {
                adapter = applicationContext.getBean(EgressAdapter.class);
            }
            break;

            case HIDDEN: {
                adapter = applicationContext.getBean(HiddenAdapter.class);
            }
            break;

            case MERGE: {
                adapter = applicationContext.getBean(MergeAdapter.class);
            }
            break;

            case POLL: {
                adapter = applicationContext.getBean(PollAdapter.class);
            }
            break;

            case PUBLISH: {
                adapter = applicationContext.getBean(PublishAdapter.class);
            }
            break;

            case PUSH: {
                adapter = applicationContext.getBean(PushAdapter.class);
            }
            break;

            case SUBSCRIBE: {
                adapter = applicationContext.getBean(SubscribeAdapter.class);
            }
            break;

            case QUEUE: {
                adapter = applicationContext.getBean(QueueAdapter.class);
            }
            break;

            default: {
                throw new Exception("ERROR: Unrecognized adapter type: "
                    + adapterType);
            }
        }
        return adapter;
    }
}
