package org.ubiquia.core.flow.component.adapter;


import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ubiquia.core.flow.model.dto.FlowMessageDto;
import org.ubiquia.core.flow.service.command.adapter.EgressAdapterCommand;

@Component
@Scope("prototype")
public class EgressAdapter extends AbstractAdapter {

    private static final Logger logger = LoggerFactory.getLogger(EgressAdapter.class);

    @Autowired
    private EgressAdapterCommand egressAdapterCommand;

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
            this.egressAdapterCommand.tryProcessInboxMessageFor(message, this);
        }
    }
}