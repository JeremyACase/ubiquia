package org.ubiquia.core.flow.service.logic.node;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.node.AbstractNode;

@Service
public class NodePassthroughLogic {

    @Autowired
    private NodeTypeLogic nodeTypeLogic;

    public Boolean isPassthrough(final AbstractNode node) {
        var isPassthrough = false;

        var nodeContext = node.getNodeContext();

        if (!this.nodeTypeLogic.nodeTypeIsValidPassthrough(nodeContext.getNodeType())
            && Objects.isNull(nodeContext.getComponent())) {
            isPassthrough = true;
        }

        return isPassthrough;
    }

}
