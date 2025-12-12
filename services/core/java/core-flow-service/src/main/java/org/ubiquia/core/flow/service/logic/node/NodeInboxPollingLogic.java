package org.ubiquia.core.flow.service.logic.node;

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
    private NodePassthroughLogic nodePassthroughLogic;

    @Autowired
    private NodeTypeLogic nodeTypeLogic;

    /**
     * Provided an adapter, determine whether or not it is a valid time for that
     * adapter to poll the inbox.
     *
     * @param node The adapter to determine validity for.
     * @return If it's valid for the adapter to poll.
     */
    public Boolean isValidToPollInbox(final AbstractNode node) {

        var context = node.getNodeContext();

        var valid = false;
        if (this.nodePassthroughLogic.isPassthrough(node)) {
            valid = true;
        } else {
            var type = context.getNodeType();
            if (this.nodeTypeLogic.nodeTypeRequiresEgressSettings(type)) {
                valid = this.hasFewerOpenMessagesThanEgressConcurrency(node);
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
     * @param node The adapter to determine validity for.
     * @return Whether or not it's a valid time to poll the inbox.
     */
    private Boolean hasFewerOpenMessagesThanEgressConcurrency(final AbstractNode node) {
        var valid = false;

        var context = node.getNodeContext();
        var maxConcurrency = context.getEgressSettings().getEgressConcurrency();
        if (context.getOpenMessages() >= maxConcurrency) {
            logger.debug("node named {} has {} open messages which is greater than or equal to "
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