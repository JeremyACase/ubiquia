package org.ubiquia.core.flow.service.logic.adapter;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;

/**
 * A service that exposes various methods common to all adapters.
 */
@Service
public class AdapterInboxPollingLogic {

    private static final Logger logger = LoggerFactory.getLogger(AdapterInboxPollingLogic.class);

    @Autowired
    private AdapterTypeLogic adapterTypeLogic;

    /**
     * Provided an adapter, determine whether or not it is a valid time for that
     * adapter to poll the inbox.
     *
     * @param adapter The adapter to determine validity for.
     * @return If it's valid for the adapter to poll.
     */
    public Boolean isValidToPollInbox(final AbstractAdapter adapter) {

        var context = adapter.getAdapterContext();

        var valid = false;
        if (context.getTemplateComponent()) {
            valid = true;
        } else {
            var type = context.getAdapterType();
            if (this.adapterTypeLogic.adapterTypeRequiresEgressSettings(type)) {
                valid = this.hasFewerOpenMessagesThanEgressConcurrency(adapter);
            } else {
                valid = true;
            }
        }
        return valid;
    }

    /**
     * Determine whether or not it's a valid time to poll the inbox for adapters
     * that have do not have agents.
     *
     * @param adapter The adapter to determine validity for.
     * @return Whether or not it's a valid time to poll the inbox.
     */
    @Transactional
    private Boolean hasFewerOpenMessagesThanEgressConcurrency(final AbstractAdapter adapter) {
        var valid = false;

        var context = adapter.getAdapterContext();
        var maxConcurrency = context.getEgressSettings().getEgressConcurrency();
        if (context.getOpenMessages() >= maxConcurrency) {
            logger.debug("Adapter named {} has {} open messages which is greater than or equal to "
                    + " max concurrency of {}; not polling...",
                context.getAdapterName(),
                context.getOpenMessages(),
                maxConcurrency);
        } else {
            valid = true;
        }
        return valid;
    }
}