package org.ubiquia.core.flow.component.node;


import com.fasterxml.jackson.core.JsonProcessingException;
import net.jimblackler.jsonschemafriend.GenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.ubiquia.common.model.ubiquia.node.QueueNodeEgress;
import org.ubiquia.core.flow.service.command.node.QueueAdapterCommand;

@Component
@Scope("prototype")
public class QueueNode extends AbstractNode {

    private static final Logger logger = LoggerFactory.getLogger(QueueNode.class);

    @Autowired
    private QueueAdapterCommand queueAdapterCommand;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void initializeBehavior() throws GenerationException, JsonProcessingException {
        super.initializeBehavior();
        super.nodeDecorator.registerPeekEndpointFor(this);
        super.nodeDecorator.registerPopEndpointFor(this);
        super.nodeDecorator.initializeBackPressurePollingFor(this);
        super.nodeDecorator.registerBackpressureEndpointFor(this);
        this.getLogger().info("...{} node initialization complete...",
            this.getNodeContext().getNodeType());
    }

    public ResponseEntity<QueueNodeEgress> peek() throws Exception {
        return this.queueAdapterCommand.peekFor(this);
    }

    public ResponseEntity<QueueNodeEgress> pop() throws Exception {
        return this.queueAdapterCommand.popFor(this);
    }
}