package org.ubiquia.core.flow.service.command.manager;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;

@Service
public class AdapterManagerCommand implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(AdapterManagerCommand.class);

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    public Logger getLogger() {
        return logger;
    }

    public void tearDown(List<AbstractAdapter> adapters) {
        for (var adapter : adapters) {
            this.tearDown(adapter);
        }
    }

    public void tearDown(AbstractAdapter adapter) {

        var adapterContext = adapter.getAdapterContext();

        logger.info("...destroying {} for graph {}...",
            adapterContext.getAdapterName(),
            adapterContext.getGraphName());

        for (var task : adapterContext.getTasks()) {
            logger.info("...cancelling recurring task: {}...", task);
            task.cancel(true);
        }
        adapterContext.getTasks().clear();

        for (var mappingInfo : adapterContext.getRegisteredMappingInfos()) {
            logger.info("...removing endpoint: {}...", mappingInfo);
            this.requestMappingHandlerMapping.unregisterMapping(mappingInfo);
        }
        adapterContext.getRegisteredMappingInfos().clear();
    }

}