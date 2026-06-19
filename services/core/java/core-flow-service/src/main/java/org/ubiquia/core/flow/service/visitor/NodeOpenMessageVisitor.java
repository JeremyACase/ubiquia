package org.ubiquia.core.flow.service.visitor;

import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.node.AbstractNode;

/** Tracks open message counts on node contexts. */
@Service
public class NodeOpenMessageVisitor {

    /** Increments the open message count for the given node. */
    public void incrementOpenMessagesFor(AbstractNode adapter) {
        var context = adapter.getNodeContext();
        var messagesCount = context.getOpenMessages() + 1;
        context.setOpenMessages(messagesCount);
    }

    /** Decrements the open message count for the given node. */
    public void decrementOpenMessagesFor(AbstractNode adapter) {
        var context = adapter.getNodeContext();
        var messagesCount = context.getOpenMessages() - 1;
        context.setOpenMessages(messagesCount);
    }
}