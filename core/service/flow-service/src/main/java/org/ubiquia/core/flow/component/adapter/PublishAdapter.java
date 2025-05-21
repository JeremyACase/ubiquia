package org.ubiquia.core.flow.component.adapter;


import io.micrometer.core.instrument.Timer;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ubiquia.core.flow.model.dto.FlowMessageDto;
import org.ubiquia.core.flow.service.io.broker.BrokerEgress;

@Component
@Scope("prototype")
public class PublishAdapter extends AbstractAdapter {

    private static final Logger logger = LoggerFactory.getLogger(PublishAdapter.class);

    @Autowired
    private BrokerEgress brokerEgress;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void initializeBehavior() {
        super.initializeBehavior();
        super.adapterDecorator.initializeInboxPollingFor(this);
        super.adapterDecorator.initializeBackPressurePollingFor(this);
        super.adapterDecorator.registerBackpressureEndpointFor(this);
        super.adapterDecorator.registerPushEndpointFor(this);
        this.getLogger().info("...{} adapter initialization complete...",
            this.getAdapterContext().getAdapterType());
    }

    @Override
    protected void tryProcessInboxMessages(List<FlowMessageDto> messages) {
        for (var message : messages) {
            this.brokerEgress.tryPublishFor(message, this);
        }
    }
}