package org.ubiquia.core.flow.component.adapter;


import com.fasterxml.jackson.core.JsonProcessingException;
import net.jimblackler.jsonschemafriend.GenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class HiddenAdapter extends AbstractAdapter {

    private static final Logger logger = LoggerFactory.getLogger(HiddenAdapter.class);

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
        super.adapterDecorator.registerPushEndpointFor(this);
        super.adapterDecorator.tryInitializeInputStimulationFor(this);
        this.getLogger().info("...{} adapter initialization complete...",
            this.getAdapterContext().getAdapterType());
    }
}