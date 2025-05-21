package org.ubiquia.core.flow.component.adapter;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.ubiquia.core.flow.model.adapter.QueueAdapterEgress;
import org.ubiquia.core.flow.service.command.adapter.QueueAdapterCommand;

@Component
@Scope("prototype")
public class QueueAdapter extends AbstractAdapter {

    private static final Logger logger = LoggerFactory.getLogger(QueueAdapter.class);

    @Autowired
    private QueueAdapterCommand queueAdapterCommand;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void initializeBehavior() {
        super.initializeBehavior();
        super.adapterDecorator.registerPeekEndpointFor(this);
        super.adapterDecorator.registerPopEndpointFor(this);
        super.adapterDecorator.initializeBackPressurePollingFor(this);
        super.adapterDecorator.registerBackpressureEndpointFor(this);
        this.getLogger().info("...{} adapter initialization complete...",
            this.getAdapterContext().getAdapterType());
    }

    public ResponseEntity<QueueAdapterEgress> peek() throws Exception {
        return this.queueAdapterCommand.peekFor(this);
    }

    public ResponseEntity<QueueAdapterEgress> pop() throws Exception {
        return this.queueAdapterCommand.popFor(this);
    }
}