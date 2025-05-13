package org.ubiquia.core.flow.service.factory;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.component.adapter.PollAdapter;
import org.ubiquia.core.flow.model.embeddable.GraphDeployment;
import org.ubiquia.core.flow.model.entity.Adapter;
import org.ubiquia.core.flow.model.entity.Agent;
import org.ubiquia.core.flow.model.entity.Graph;
import org.ubiquia.core.flow.model.enums.AdapterType;
import org.ubiquia.core.flow.service.builder.AdapterBuilder;

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
        final Adapter adapterEntity,
        final Graph graphEntity,
        final GraphDeployment graphDeployment)
        throws Exception {

        logger.info("...building an adapter named {} for graph {}...",
            adapterEntity.getAdapterName(),
            graphEntity.getGraphName());

        var adapter = this.makeAdapterByType(adapterEntity.getAdapterType());
        this.adapterBuilder.buildAdapter(adapter, adapterEntity, graphEntity, graphDeployment);

        logger.info("...completed building adapter {} for graph {}...",
            adapterEntity.getAdapterName(),
            adapterEntity.getGraph().getGraphName());
        return adapter;
    }

    /**
     * Make an adapter for an agent.
     *
     * @param agentEntity     The agent to make an adapter for.
     * @param graphDeployment The graph deployment requesting the adapter.
     * @return An adapter.
     * @throws Exception Exceptions from building the adapter.
     */
    public AbstractAdapter makeAdapterFor(
        final Agent agentEntity,
        final GraphDeployment graphDeployment)
        throws Exception {

        logger.info("...building an adapter for graph {} and data transform {}...",
            agentEntity.getGraph().getGraphName(),
            agentEntity.getAgentName());

        if (Objects.isNull(agentEntity.getAdapter())) {
            throw new Exception("ERROR: Cannot build an adapter from a transform with "
                + "a null adapter!");
        }

        var adapter = this.makeAdapterByType(
            agentEntity.getAdapter().getAdapterType());
        this.adapterBuilder.buildAdapter(
            adapter,
            agentEntity.getAdapter(),
            agentEntity.getGraph(),
            graphDeployment);

        logger.info("...completed building adapter for graph {} and data transform named {}...",
            agentEntity.getGraph().getGraphName(),
            agentEntity.getAgentName());

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

            case POLL: {
                adapter = applicationContext.getBean(PollAdapter.class);
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
