package org.ubiquia.core.flow.service.logic.node;

import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.enums.NodeType;

/**
 * A service that exposes adapter type logic.
 */
@Service
public class NodeTypeLogic {

    /**
     * Return whether or not this type of adapter requires egress settings.
     *
     * @param nodeType The type of adapter to verify.
     * @return Whether or not the adapter requires egress settings.
     */
    public Boolean nodeTypeRequiresEgressSettings(final NodeType nodeType) {

        return nodeType.equals(NodeType.PUSH)
            || nodeType.equals(NodeType.EGRESS)
            || nodeType.equals(NodeType.HIDDEN)
            || nodeType.equals(NodeType.SUBSCRIBE)
            || nodeType.equals(NodeType.MERGE);
    }

    public Boolean isTerminalNodeType(final NodeType nodeType) {

        var terminalNodeType =
            nodeType.equals(NodeType.QUEUE)
                || nodeType.equals(NodeType.PUBLISH)
                || nodeType.equals(NodeType.EGRESS);

        return terminalNodeType;
    }

    public Boolean nodeTypeIsValidPassthrough(final NodeType nodeType) {
        return nodeType.equals(NodeType.HIDDEN)
            || nodeType.equals(NodeType.PUSH)
            || nodeType.equals(NodeType.MERGE)
            || nodeType.equals(NodeType.POLL)
            || nodeType.equals(NodeType.SUBSCRIBE);
    }

    /**
     * Return whether or not this type of adapter requires an endpoint.
     *
     * @param nodeType The type of adapter to verify.
     * @return Whether or not the adapter requires an endpoint.
     */
    public Boolean nodeTypeRequiresEndpoint(final NodeType nodeType) {

        return !nodeType.equals(NodeType.QUEUE)
            && !nodeType.equals(NodeType.PUBLISH);
    }
}
