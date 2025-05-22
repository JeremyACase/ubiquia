package org.ubiquia.core.flow.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.models.dto.FlowEventDto;
import org.ubiquia.common.models.entity.FlowEvent;

@RestController
@RequestMapping("/ubiquia/event")
public class FlowEventController extends GenericEntityController<FlowEvent, FlowEventDto> {

    private static final Logger logger = LoggerFactory.getLogger(FlowEventController.class);

    @Override
    public Logger getLogger() {
        return logger;
    }
}
