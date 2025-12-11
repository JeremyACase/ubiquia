package org.ubiquia.core.flow.component.node;


import com.fasterxml.jackson.core.JsonProcessingException;
import net.jimblackler.jsonschemafriend.GenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class HiddenNode extends AbstractNode {

    private static final Logger logger = LoggerFactory.getLogger(HiddenNode.class);

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void initializeBehavior() throws GenerationException, JsonProcessingException {
        super.initializeBehavior();
        super.nodeDecorator.initializeInboxPollingFor(this);
        super.nodeDecorator.initializeBackPressurePollingFor(this);
        super.nodeDecorator.initializeOutputLogicFor(this);
        super.nodeDecorator.registerBackpressureEndpointFor(this);
        super.nodeDecorator.registerPushEndpointFor(this);
        super.nodeDecorator.tryInitializeInputStimulationFor(this);
        this.getLogger().info("...{} node initialization complete...",
            this.getNodeContext().getNodeType());
    }
}