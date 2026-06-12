package org.ubiquia.core.flow.service.logic.node;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.component.node.AbstractNode;

@Service
public class NodePassthroughLogic {

    @Autowired
    private NodeTypeLogic nodeTypeLogic;

    public Boolean isPassthrough(final AbstractNode node) {
        var isPassthrough = false;

        var nodeContext = node.getNodeContext();

        var type = nodeContext.getNodeType();
        var isHidden = NodeType.HIDDEN.equals(type);
        if (!this.nodeTypeLogic.isTerminalNodeType(type) && !isHidden) {

            if (this.nodeTypeLogic.nodeTypeIsValidPassthrough(type)
                && Objects.isNull(nodeContext.getComponent())) {
                isPassthrough = true;
            }
        }

        return isPassthrough;
    }

    public Boolean isPassthrough(final Node node) {
        var isPassthrough = false;

        var type = node.getNodeType();
        if (!this.nodeTypeLogic.isTerminalNodeType(type)) {

            if (!this.nodeTypeLogic.nodeTypeIsValidPassthrough(type)
                && Objects.isNull(node.getComponent())) {
                isPassthrough = true;
            }
        }

        return isPassthrough;
    }
}
