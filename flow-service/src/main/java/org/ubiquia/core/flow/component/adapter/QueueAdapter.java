package org.ubiquia.core.flow.component.adapter;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.ubiquia.core.flow.model.adapter.QueueAdapterEgress;
import org.ubiquia.core.flow.service.logic.adapter.QueueAdapterLogic;

@Component
@Scope("prototype")
public class QueueAdapter extends AbstractAdapter {

    @Autowired
    private QueueAdapterLogic queueAdapterLogic;

    public ResponseEntity<QueueAdapterEgress> peek() throws Exception {
        return this.queueAdapterLogic.peekFor(this);
    }

    public ResponseEntity<QueueAdapterEgress> pop() throws Exception {
        return this.queueAdapterLogic.popFor(this);
    }
}