package org.ubiquia.core.flow.service.builder;


import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.adapter.AAdapter;
import org.ubiquia.core.flow.model.adapter.AdapterContext;
import org.ubiquia.core.flow.model.dto.AdapterDto;
import org.ubiquia.core.flow.model.embeddable.GraphDeployment;
import org.ubiquia.core.flow.model.entity.Graph;
import org.ubiquia.core.flow.model.enums.AgentType;

/**
 * A service dedicated to building adapters for AMIGOS instances.
 */
@Service
public class AdapterContextBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AdapterContextBuilder.class);

    public AdapterContext buildAdapterContext(
        final AdapterDto adapterData,
        final Graph graphEntity,
        final GraphDeployment graphDeployment) {

        var context = new AdapterContext();

        context.setAdapterId(adapterData.getId());
        context.setAdapterName(adapterData.getAdapterName());
        context.setAdapterType(adapterData.getAdapterType());
        context.setAdapterSettings(adapterData.getAdapterSettings());
        context.setBackpressurePollRatePerMinute(
            TimeUnit.MINUTES.toMillis(1)
                / adapterData
                .getAdapterSettings()
                .getBackpressurePollFrequencyMilliseconds());
        context.setBrokerSettings(adapterData.getBrokerSettings());
        context.setEgressSettings(adapterData.getEgressSettings());
        if (Objects.nonNull(adapterData.getAgent())) {
            context.setAgentName(adapterData.getAgent().getAgentName());
        }
        context.setGraphName(graphEntity.getGraphName());
        context.setGraphSettings(graphDeployment.getGraphSettings());
        context.setPollSettings(adapterData.getPollSettings());

        if (Objects.nonNull(adapterData.getAgent())
            && adapterData.getAgent().getDataTransformType().equals(AgentType.TEMPLATE)) {
            context.setTemplateTransform(true);
        } else {
            context.setTemplateTransform(false);
        }

        return context;
    }
}
