package org.ubiquia.core.flow.service.visitor;


import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.node.AbstractNode;

@Service
public class NodeOpenMessageVisitor {

    public void incrementOpenMessagesFor(AbstractNode adapter) {
        var context = adapter.getNodeContext();
        var messagesCount = context.getOpenMessages() + 1;
        context.setOpenMessages(messagesCount);
    }

    public void decrementOpenMessagesFor(AbstractNode adapter) {
        var context = adapter.getNodeContext();
        var messagesCount = context.getOpenMessages() - 1;
        context.setOpenMessages(messagesCount);
    }
}