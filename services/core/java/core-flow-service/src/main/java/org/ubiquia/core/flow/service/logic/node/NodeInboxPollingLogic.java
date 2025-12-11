package org.ubiquia.core.flow.service.logic.node;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.node.AbstractNode;

/**
 * A service that exposes various methods common to all adapters.
 */
@Service
public class NodeInboxPollingLogic {

    private static final Logger logger = LoggerFactory.getLogger(NodeInboxPollingLogic.class);

    @Autowired
    private NodeTypeLogic nodeTypeLogic;

    /**
     * Provided an adapter, determine whether or not it is a valid time for that
     * adapter to poll the inbox.
     *
     * @param adapter The adapter to determine validity for.
     * @return If it's valid for the adapter to poll.
     */
    public Boolean isValidToPollInbox(final AbstractNode adapter) {

        var context = adapter.getNodeContext();

        var valid = false;
        if (context.hasTemplateComponent()) {
            valid = true;
        } else {
            var type = context.getNodeType();
            if (this.nodeTypeLogic.nodeTypeRequiresEgressSettings(type)) {
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
    private Boolean hasFewerOpenMessagesThanEgressConcurrency(final AbstractNode adapter) {
        var valid = false;

        var context = adapter.getNodeContext();
        var maxConcurrency = context.getEgressSettings().getEgressConcurrency();
        if (context.getOpenMessages() >= maxConcurrency) {
            logger.debug("Adapter named {} has {} open messages which is greater than or equal to "
                    + " max concurrency of {}; not polling...",
                context.getNodeName(),
                context.getOpenMessages(),
                maxConcurrency);
        } else {
            valid = true;
        }
        return valid;
    }
}