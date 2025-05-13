package org.ubiquia.core.flow.component.adapter;


import org.springframework.beans.factory.annotation.Autowired;
import org.ubiquia.core.flow.model.adapter.AdapterContext;
import org.ubiquia.core.flow.service.command.adapter.AdapterInboxMessageCommand;
import org.ubiquia.core.flow.service.command.adapter.AdapterInboxPollCommand;
import org.ubiquia.core.flow.service.logic.adapter.AdapterInboxPollingLogic;

/**
 * An abstract class that can be used to adapt to Agents in Ubiquia. There are different
 * types of adapters; each inherits from this abstract base class.
 * Adapters are built at runtime using data from the database. They can be torn down or deployed
 * dynamically when a graph is similarly torn down or deployed.
 * All adapters do very-different things (i.e., poll versus POST.) As such, some adapters
 * will constantly monitor their "backpressure", while others will not.
 */
public abstract class AbstractAdapter {

    private AdapterContext adapterContext;

    @Autowired
    private AdapterInboxMessageCommand adapterInboxMessageCommand;

    @Autowired
    private AdapterInboxPollCommand adapterInboxPollCommand;

    @Autowired
    private AdapterInboxPollingLogic adapterInboxPollingLogic;

    public AdapterContext getAdapterContext() {
        return adapterContext;
    }

    public void setAdapterContext(AdapterContext adapterContext) {
        this.adapterContext = adapterContext;
    }

    public void tryPollInbox() {

        if (this.adapterInboxPollingLogic.isValidToPollInbox(this)) {
            var messages = this.adapterInboxPollCommand.tryPollInboxFor(this);
            for (var message : messages) {
                this.adapterInboxMessageCommand.tryProcessInboxMessageFor(message, this);
            }
        }
    }
}