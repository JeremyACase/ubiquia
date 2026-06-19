package org.ubiquia.core.flow.service.logic.node;

import java.util.Objects;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.core.flow.component.node.AbstractNode;

/** Determines whether a node should produce a simulated (non-component) output payload. */
@Service
public class NodeSimulatedOutputLogic {

    /** Returns true if the node's output payload should be simulated rather than computed. */
    public Boolean isSimulatedResponsePayload(final AbstractNode node) {
        var component = node.getNodeContext().getComponent();
        var isHiddenWithNoComponent = Objects.isNull(component)
            && NodeType.HIDDEN.equals(node.getNodeContext().getNodeType());
        var isTemplateComponent = Objects.nonNull(component)
            && ComponentType.TEMPLATE.equals(component.getComponentType());
        return isHiddenWithNoComponent || isTemplateComponent;
    }
}
