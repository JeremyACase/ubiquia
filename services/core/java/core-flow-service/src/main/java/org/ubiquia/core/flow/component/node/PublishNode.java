package org.ubiquia.core.flow.component.node;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import net.jimblackler.jsonschemafriend.GenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.core.flow.service.io.broker.BrokerEgress;

@Component
@Scope("prototype")
public class PublishNode extends AbstractNode {

    private static final Logger logger = LoggerFactory.getLogger(PublishNode.class);

    @Autowired
    private BrokerEgress brokerEgress;

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

    @Override
    protected void tryProcessInboxMessages(List<FlowMessage> messages) {
        for (var message : messages) {
            this.brokerEgress.tryPublishFor(message, this);
        }
    }
}