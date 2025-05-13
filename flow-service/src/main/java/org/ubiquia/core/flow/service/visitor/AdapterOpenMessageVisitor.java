package org.ubiquia.core.flow.service.visitor;


import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;

@Service
public class AdapterOpenMessageVisitor {

    public void incrementOpenMessagesFor(AbstractAdapter adapter) {
        var context = adapter.getAdapterContext();
        var messagesCount = context.getOpenMessages() + 1;
        context.setOpenMessages(messagesCount);
    }

    public void decrementOpenMessagesFor(AbstractAdapter adapter) {
        var context = adapter.getAdapterContext();
        var messagesCount = context.getOpenMessages() - 1;
        context.setOpenMessages(messagesCount);
    }
}