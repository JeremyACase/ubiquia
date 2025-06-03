package org.ubiquia.core.flow.service.builder.adapter;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.AdapterDto;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.entity.Graph;
import org.ubiquia.common.model.ubiquia.enums.AgentType;
import org.ubiquia.core.flow.model.adapter.AdapterContext;
import org.ubiquia.core.flow.service.logic.adapter.AdapterTypeLogic;

@Service
public class AdapterContextBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AdapterContextBuilder.class);

    @Autowired
    private AdapterTypeLogic adapterTypeLogic;

    public AdapterContext buildAdapterContext(
        final AdapterDto adapterData,
        final Graph graphEntity,
        final GraphDeployment graphDeployment)
        throws URISyntaxException {

        var context = new AdapterContext();

        context.setAdapterName(adapterData.getAdapterName());
        context.setAdapterId(adapterData.getId());
        context.setAdapterType(adapterData.getAdapterType());
        context.setAdapterSettings(adapterData.getAdapterSettings());
        context.setBackpressurePollRatePerMinute(
            this.getBackPressurePollRatePerMinute(adapterData));
        context.setBrokerSettings(adapterData.getBrokerSettings());
        context.setEgressSettings(adapterData.getEgressSettings());
        context.setGraphName(graphEntity.getGraphName());
        context.setGraphSettings(graphDeployment.getGraphSettings());
        context.setPollSettings(adapterData.getPollSettings());

        if (Objects.nonNull(adapterData.getAgent())
            && adapterData.getAgent().getAgentType().equals(AgentType.TEMPLATE)) {
            context.setTemplateAgent(true);
        } else {
            context.setTemplateAgent(false);
        }

        if (Objects.nonNull(adapterData.getAgent())) {
            context.setAgentName(adapterData.getAgent().getAgentName());
        }

        this.trySetAdapterContextEndpoint(context, adapterData);

        return context;
    }

    private Long getBackPressurePollRatePerMinute(final AdapterDto adapterData) {

        var x = TimeUnit.MINUTES.toMillis(1);
        var y = adapterData.getAdapterSettings().getBackpressurePollFrequencyMilliseconds();

        return x / y;
    }

    private URI getAgentUriFrom(final AdapterDto adapter)
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

    private void trySetAdapterContextEndpoint(
        AdapterContext adapterContext,
        final AdapterDto adapterData)
        throws URISyntaxException {

        if (!adapterData.getAdapterSettings().getIsPassthrough()) {

            if (this.adapterTypeLogic.adapterTypeRequiresAgent(
                adapterData.getAdapterType())) {


                switch (adapterData.getAgent().getAgentType()) {

                    // If we have a Kubernetes service to use...
                    case POD: {
                        var uri = this.getAgentUriFrom(adapterData);
                        adapterContext.setEndpointUri(uri);
                    } break;

                    // ...use full endpoint for NONE type...
                    case NONE: {
                        var uri = new URI(adapterData.getEndpoint());
                        adapterContext.setEndpointUri(uri);
                    } break;

                    // ...else just set to null
                    default: {
                        adapterContext.setEndpointUri(null);
                    }

                }
            } else if (this.adapterTypeLogic.adapterTypeRequiresEndpoint(
                adapterData.getAdapterType())) {

                var uri = new URI(adapterData.getEndpoint());
                adapterContext.setEndpointUri(uri);
            }
        }
    }
}
