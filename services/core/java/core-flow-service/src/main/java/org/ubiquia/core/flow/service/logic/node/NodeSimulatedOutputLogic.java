package org.ubiquia.core.flow.service.logic.node;

import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.node.AbstractNode;

@Service
public class NodeSimulatedOutputLogic {

    public Boolean isSimulatedResponsePayload(final AbstractNode node) {
        var isSimulateResponsePayload = node
            .getNodeContext()
            .getNodeSettings()
            .getSimulateOutputPayload();
        return isSimulateResponsePayload;
    }
}
