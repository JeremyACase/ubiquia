package org.ubiquia.core.flow.service.builder.adapter;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.logic.service.builder.AdapterEndpointRecordBuilder;
import org.ubiquia.core.flow.model.adapter.AdapterContext;
import org.ubiquia.common.model.ubiquia.dto.Adapter;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;
import org.ubiquia.core.flow.service.logic.adapter.AdapterTypeLogic;

@Service
public class AdapterContextBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AdapterContextBuilder.class);

    @Autowired
    private AdapterEndpointRecordBuilder adapterEndpointRecordBuilder;

    @Autowired
    private AdapterTypeLogic adapterTypeLogic;

    public AdapterContext buildAdapterContext(
        final Adapter adapterData,
        final GraphEntity graphEntity,
        final GraphDeployment graphDeployment)
        throws URISyntaxException {

        var context = new AdapterContext();

        context.setAdapterName(adapterData.getName());
        context.setAdapterId(adapterData.getId());
        context.setAdapterType(adapterData.getAdapterType());
        context.setAdapterSettings(adapterData.getAdapterSettings());
        context.setBackpressurePollRatePerMinute(
            this.getBackPressurePollRatePerMinute(adapterData));
        context.setBrokerSettings(adapterData.getBrokerSettings());
        context.setEgressSettings(adapterData.getEgressSettings());
        context.setGraphName(graphEntity.getName());
        context.setGraphSettings(graphDeployment.getGraphSettings());
        context.setPollSettings(adapterData.getPollSettings());

        if (Objects.nonNull(adapterData.getComponent())
            && adapterData.getComponent().getComponentType().equals(ComponentType.TEMPLATE)) {
            context.setTemplateComponent(true);
        } else {
            context.setTemplateComponent(false);
        }

        if (Objects.nonNull(adapterData.getComponent())) {
            context.setComponentName(adapterData.getComponent().getName());
        }

        this.trySetAdapterContextEndpoint(context, adapterData);

        return context;
    }

    private Long getBackPressurePollRatePerMinute(final Adapter adapterData) {

        var x = TimeUnit.MINUTES.toMillis(1);
        var y = adapterData.getAdapterSettings().getBackpressurePollFrequencyMilliseconds();

        return x / y;
    }

    private void trySetAdapterContextEndpoint(
        AdapterContext adapterContext,
        final Adapter adapterData)
        throws URISyntaxException {

        if (!adapterData.getAdapterSettings().getIsPassthrough()) {

            if (this.adapterTypeLogic.adapterTypeRequiresComponent(
                adapterData.getAdapterType())) {


                switch (adapterData.getComponent().getComponentType()) {

                    // If we have a Kubernetes service to use...
                    case POD: {
                        var uri = this.adapterEndpointRecordBuilder.getAgentUriFrom(adapterData);
                        adapterContext.setEndpointUri(uri);
                    }
                    break;

                    // ...use full endpoint for NONE type...
                    case NONE: {
                        var uri = new URI(adapterData.getEndpoint());
                        adapterContext.setEndpointUri(uri);
                    }
                    break;

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
