package org.ubiquia.core.flow.service.builder.adapter;


import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.logic.service.builder.AdapterEndpointRecordBuilder;
import org.ubiquia.common.model.ubiquia.dto.AdapterDto;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.entity.Adapter;
import org.ubiquia.common.model.ubiquia.entity.Graph;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.service.decorator.adapter.override.AdapterOverrideDecorator;
import org.ubiquia.core.flow.service.logic.adapter.AdapterTypeLogic;
import org.ubiquia.core.flow.service.mapper.AdapterDtoMapper;

@Service
public class AdapterBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AdapterBuilder.class);
    @Autowired
    private AdapterDtoMapper adapterDtoMapper;
    @Autowired
    private AdapterContextBuilder adapterContextBuilder;
    @Autowired
    private AdapterOverrideDecorator adapterOverrideDecorator;
    @Autowired
    private AdapterTagBuilder adapterTagBuilder;
    @Autowired
    private AdapterEndpointRecordBuilder adapterEndpointRecordBuilder;
    @Autowired
    private AdapterTypeLogic adapterTypeLogic;

    /**
     * Build the provided adapter by using data from the database.
     *
     * @param adapter       The adapter to build.
     * @param adapterEntity The adapter's data from the database.
     * @param graphEntity   The graph's data from the database.
     * @throws URISyntaxException Exceptions from setting the URL that the adapter should
     *                            communicate with.
     */
    @Transactional
    public void buildAdapter(
        AbstractAdapter adapter,
        final Adapter adapterEntity,
        final Graph graphEntity,
        final GraphDeployment graphDeployment)
        throws URISyntaxException,
        JsonProcessingException,
        IllegalAccessException {

        logger.info("...building {} adapter named {} for graph {} with settings {}...",
            adapterEntity.getAdapterType(),
            adapterEntity.getAdapterName(),
            graphEntity.getGraphName(),
            graphDeployment.getGraphSettings());

        var adapterData = this.adapterDtoMapper.map(adapterEntity);

        var context = this.adapterContextBuilder.buildAdapterContext(
            adapterData,
            graphEntity,
            graphDeployment);
        adapter.setAdapterContext(context);

        var tags = this.adapterTagBuilder.buildAdapterTags(adapter);
        context.setTags(tags);

        this.adapterOverrideDecorator.tryOverrideBaselineValues(
            adapterData,
            adapterEntity.getOverrideSettings().stream().toList(),
            graphDeployment);

        this.trySetAdapterEndpoint(adapter, adapterData);
        adapter.initializeBehavior();

        logger.info("...built adapter...");
    }


    /**
     * Attempt to set an adapter's endpoint.
     *
     * @param adapter     The adapter to set.
     * @param adapterData The adapter data from the database.
     * @throws URISyntaxException Exceptions from parsing endpoints.
     */
    private void trySetAdapterEndpoint(AbstractAdapter adapter, final AdapterDto adapterData)
        throws URISyntaxException {

        var agentData = adapterData.getAgent();
        var adapterContext = adapter.getAdapterContext();
        if (!adapterData.getAdapterSettings().getIsPassthrough()) {

            if (this.adapterTypeLogic.adapterTypeRequiresAgent(
                adapterData.getAdapterType())) {

                switch (agentData.getAgentType()) {

                    // If we have a Kubernetes service to use...
                    case POD: {
                        adapterContext.setEndpointUri(
                            this.adapterEndpointRecordBuilder.getAgentUriFrom(adapterData));
                    }
                    break;

                    // ...use full endpoint for NONE type...
                    case NONE: {
                        adapterContext.setEndpointUri(new URI(adapterData.getEndpoint()));
                    }
                    break;

                    // ...else just set to null
                    default: {
                        adapterContext.setEndpointUri(null);
                    }

                }
            } else if (this.adapterTypeLogic.adapterTypeRequiresEndpoint(adapterData.getAdapterType())) {
                adapterContext.setEndpointUri(new URI(adapterData.getEndpoint()));
            }
        }
    }
}
