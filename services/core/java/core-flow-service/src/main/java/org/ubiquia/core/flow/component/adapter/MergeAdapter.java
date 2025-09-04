package org.ubiquia.core.flow.component.adapter;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import net.jimblackler.jsonschemafriend.GenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
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
    public void initializeBehavior() throws GenerationException, JsonProcessingException {
        super.initializeBehavior();
        super.adapterDecorator.initializeInboxPollingFor(this);
        super.adapterDecorator.initializeBackPressurePollingFor(this);
        super.adapterDecorator.initializeOutputLogicFor(this);
        super.adapterDecorator.registerBackpressureEndpointFor(this);
        this.getLogger().info("...{} adapter initialization complete...",
            this.getAdapterContext().getAdapterType());
    }

    @Override
    protected void tryProcessInboxMessages(List<FlowMessage> messages) {
        for (var message : messages) {
            this.mergeAdapterCommand.tryProcessMessageFor(message, this);
        }
    }
}