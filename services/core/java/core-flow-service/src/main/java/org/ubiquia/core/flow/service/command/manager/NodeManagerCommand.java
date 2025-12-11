package org.ubiquia.core.flow.service.command.manager;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.core.flow.component.node.AbstractNode;

@Service
public class NodeManagerCommand implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(NodeManagerCommand.class);

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    public Logger getLogger() {
        return logger;
    }

    public void tearDown(List<AbstractNode> nodes) {
        for (var node : nodes) {
            this.tearDown(node);
        }
    }

    public void tearDown(AbstractNode node) {

        var nodeContext = node.getNodeContext();

        logger.info("...destroying {} for graph {}...",
            nodeContext.getNodeName(),
            nodeContext.getGraphName());

        for (var task : nodeContext.getTasks()) {
            logger.info("...cancelling recurring task: {}...", task);
            task.cancel(true);
        }
        nodeContext.getTasks().clear();

        for (var mappingInfo : nodeContext.getRegisteredMappingInfos()) {
            logger.debug("...unregistering mapping info: {}...", mappingInfo);
            this.requestMappingHandlerMapping.unregisterMapping(mappingInfo);
        }

        nodeContext.getRegisteredMappingInfos().clear();
    }
}