package org.ubiquia.core.flow.service.decorator.node.override;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.core.flow.service.decorator.override.GenericOverrideDecorator;

@Service
public class NodeOverrideDecorator extends GenericOverrideDecorator<Node> {

    private static final Logger logger = LoggerFactory.getLogger(NodeOverrideDecorator.class);

    @Override
    public Logger getLogger() {
        return logger;
    }

}
