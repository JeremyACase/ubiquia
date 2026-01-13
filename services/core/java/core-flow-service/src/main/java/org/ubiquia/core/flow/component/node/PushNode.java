package org.ubiquia.core.flow.component.node;


import com.fasterxml.jackson.core.JsonProcessingException;
import net.jimblackler.jsonschemafriend.GenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class PushNode extends AbstractNode {

    private static final Logger logger = LoggerFactory.getLogger(PushNode.class);

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void initializeBehavior() throws GenerationException, JsonProcessingException {
        super.initializeBehavior();
        super.nodeDecorator.registerPushEndpointFor(this);
        super.nodeDecorator.initializeOutputLogicFor(this);
        super.nodeDecorator.tryInitializeInputStimulationFor(this);
        super.payloadModelValidator.tryInitializeInputPayloadSchema(
            super.getNodeContext());
        this.getLogger().info("...{} node initialization complete...",
            this.getNodeContext().getNodeType());
    }
}