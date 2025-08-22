package org.ubiquia.core.flow.component.adapter;


import net.jimblackler.jsonschemafriend.GenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class PushAdapter extends AbstractAdapter {

    private static final Logger logger = LoggerFactory.getLogger(PushAdapter.class);

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void initializeBehavior() throws GenerationException {
        super.initializeBehavior();
        super.adapterDecorator.registerPushEndpointFor(this);
        super.adapterDecorator.initializeOutputLogicFor(this);
        super.adapterDecorator.tryInitializeInputStimulationFor(this);
        super.payloadModelValidator.tryInitializeInputPayloadSchema(
            super.getAdapterContext().getAdapterId());
        this.getLogger().info("...{} adapter initialization complete...",
            this.getAdapterContext().getAdapterType());
    }
}