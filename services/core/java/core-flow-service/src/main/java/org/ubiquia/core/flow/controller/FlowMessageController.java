package org.ubiquia.core.flow.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.core.flow.service.registrar.FlowMessageRegistrar;

@RestController
@RequestMapping("/ubiquia/core/flow-service/flow-message")
public class FlowMessageController implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(FlowMessageController.class);

    @Autowired
    private FlowMessageRegistrar flowMessageRegistrar;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @PostMapping("/receive")
    public void receive(@RequestBody final FlowMessage flowMessage) {
        logger.info("Received incoming flow message for node {}.",
            flowMessage.getTargetNode().getId());
        this.flowMessageRegistrar.tryRegisterFlowMessage(flowMessage);
    }
}
