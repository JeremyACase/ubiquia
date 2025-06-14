package org.ubiquia.core.flow.component.adapter;


import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ubiquia.common.model.ubiquia.dto.FlowMessageDto;
import org.ubiquia.core.flow.service.command.adapter.MergeAdapterCommand;

@Component
@Scope("prototype")
public class MergeAdapter extends AbstractAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MergeAdapter.class);

    @Autowired
    private MergeAdapterCommand mergeAdapterCommand;

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
        this.getLogger().info("...{} adapter initialization complete...",
            this.getAdapterContext().getAdapterType());
    }

    @Override
    protected void tryProcessInboxMessages(List<FlowMessageDto> messages) {
        for (var message : messages) {
            this.mergeAdapterCommand.tryProcessMessageFor(message, this);
        }
    }
}