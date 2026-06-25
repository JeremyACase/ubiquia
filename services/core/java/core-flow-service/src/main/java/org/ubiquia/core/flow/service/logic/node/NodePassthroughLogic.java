package org.ubiquia.core.flow.service.logic.node;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.core.flow.component.node.AbstractNode;

/** Determines whether a node acts as a passthrough (no component processing). */
@Service
public class NodePassthroughLogic {

    @Autowired
    private NodeTypeLogic nodeTypeLogic;

    /** Returns true if the given node instance is a passthrough node. */
    public Boolean isPassthrough(final AbstractNode node) {
        var isPassthrough = false;

        var nodeContext = node.getNodeContext();

        var type = nodeContext.getNodeType();
        if (!this.nodeTypeLogic.isTerminalNodeType(type)) {

            if (this.nodeTypeLogic.nodeTypeIsValidPassthrough(type)
                && Objects.isNull(nodeContext.getComponent())) {
                isPassthrough = true;
            }
        }

        return isPassthrough;
    }

    /** Returns true if the given node DTO is a passthrough node. */
    public Boolean isPassthrough(final Node node) {
        var isPassthrough = false;

        var type = node.getNodeType();
        if (!this.nodeTypeLogic.isTerminalNodeType(type)) {

            if (!this.nodeTypeLogic.nodeTypeIsValidPassthrough(type)
                && Objects.isNull(node.getTargetComponent())) {
                isPassthrough = true;
            }
        }

        return isPassthrough;
    }
}
