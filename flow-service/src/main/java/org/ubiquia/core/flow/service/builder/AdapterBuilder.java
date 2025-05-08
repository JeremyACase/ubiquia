package org.ubiquia.core.flow.service.builder;


import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.adapter.AAdapter;
import org.ubiquia.core.flow.model.adapter.AdapterContext;
import org.ubiquia.core.flow.model.dto.AdapterDto;
import org.ubiquia.core.flow.model.embeddable.GraphDeployment;
import org.ubiquia.core.flow.model.entity.Adapter;
import org.ubiquia.core.flow.model.entity.Graph;
import org.ubiquia.core.flow.service.mapper.AdapterDtoMapper;

/**
 * A service dedicated to building adapters for AMIGOS instances.
 */
@Service
public class AdapterBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AdapterBuilder.class);
    @Autowired
    private AdapterDtoMapper adapterDTOMapper;
    @Autowired
    private AdapterContextBuilder adapterContextBuilder;
    @Autowired
    private AdapterOverrideDecorator adapterOverrideDecorator;
    @Autowired
    private AdapterTagBuilder adapterTagBuilder;
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
        AAdapter adapter,
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

        var adapterData = this.adapterDTOMapper.map(adapterEntity);

        var context = this.adapterContextBuilder.buildAdapterContext(
            adapterData,
            graphEntity,
            graphDeployment);
        context.setTags(this.adapterTagBuilder.buildAdapterTags(adapter));

        adapter.setAdapterContext(context);

        this.adapterOverrideDecorator.tryOverrideBaselineValues(
            adapterData,
            adapterEntity.getOverrideSettings().stream().toList(),
            graphDeployment);
        logger.info("...built adapter...");
    }

    /**
     * Get the adapter's Data Transform URI.
     *
     * @param adapter The adapter to get a URI endpoint from.
     * @return The URI of the endpoint.
     * @throws URISyntaxException Exception from creating a URI.
     */
    private URI getTransformURIFrom(final AdapterDto adapter)
        throws URISyntaxException {

        var uri = new URI("http",
            null,
            adapter.getAgent().getAgentName(),
            adapter.getAgent().getPort(),
            adapter.getEndpoint(),
            null,
            null
        );

        return uri;
    }

    /**
     * Attempt to set an adapter's endpoint.
     *
     * @param adapter     The adapter to set.
     * @param adapterData The adapter data from the database.
     * @throws URISyntaxException Exceptions from parsing endpoints.
     */
    private void trySetAdapterEndpoint(AAdapter adapter, final AdapterDto adapterData)
        throws URISyntaxException {

        if (!adapterData.getAdapterSettings().getIsPassthrough()) {

            if (this.adapterTypeLogic.adapterTypeRequiresDataTransform(
                adapterData.getAdapterType())) {

                switch (adapterData.getDataTransform().getDataTransformType()) {

                    // If we have a Kubernetes service to use...
                    case POD: {
                        adapter.setEndpointURI(this.getTransformURIFrom(adapterData));
                    }
                    break;

                    // ...use full endpoint for NONE type...
                    case NONE: {
                        adapter.setEndpointURI(new URI(adapterData.getEndpoint()));
                    }
                    break;

                    // ...else just set to null
                    default: {
                        adapter.setEndpointURI(null);
                    }

                }
            } else if (this.adapterTypeLogic.adapterTypeRequiresEndpoint(
                adapterData.getAdapterType())) {
                adapter.setEndpointURI(new URI(adapterData.getEndpoint()));
            }
        }
    }
}
