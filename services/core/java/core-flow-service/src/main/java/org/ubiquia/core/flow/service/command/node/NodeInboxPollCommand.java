package org.ubiquia.core.flow.service.command.node;

import io.micrometer.core.instrument.Timer;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.core.flow.component.node.AbstractNode;
import org.ubiquia.core.flow.service.io.Inbox;
import org.ubiquia.core.flow.service.telemetry.MicroMeterHelper;

/**
 * A service that exposes various methods common to all adapters.
 */
@Service
public class NodeInboxPollCommand {

    private static final Logger logger = LoggerFactory.getLogger(NodeInboxPollCommand.class);

    @Autowired
    private Inbox inbox;

    @Autowired
    private NodeProcessInboxMessageCommand nodeProcessInboxMessageCommand;

    @Autowired(required = false)
    private MicroMeterHelper microMeterHelper;

    public Logger getLogger() {
        return logger;
    }

    @Transactional
    public List<FlowMessage> tryPollInboxFor(final AbstractNode node) {
        var nodeContext = node.getNodeContext();
        this.getLogger().debug("Node {} of graph {} with is polling inbox...",
            nodeContext.getNodeName(),
            nodeContext.getGraph().getName());

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterHelper)) {
            sample = this.microMeterHelper.startSample();
        }

        var messages = new ArrayList<FlowMessage>();
        try {
            var queriedMessages = this.inbox.tryQueryInboxMessagesFor(node);
            messages.addAll(queriedMessages);
        } catch (Exception e) {
            this.getLogger().error("ERROR with inbox polling: {}", e.getMessage());
        }

        if (Objects.nonNull(sample)) {
            this.microMeterHelper.endSample(sample, "pollInbox", nodeContext.getTags());
        }
        this.getLogger().debug("...finished polling inbox.");

        return messages;
    }
}