package org.ubiquia.core.flow.controller;

import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.implementation.service.clock.ClockService;

/**
 * A controller that exposes simulation capabilities for the flow service.
 * Only active when the deployment mode is not PROD.
 */
@RestController
@RequestMapping("/ubiquia/core/flow-service/simulation")
@ConditionalOnExpression("'${ubiquia.mode:PROD}' != 'PROD'")
public class SimulationController {

    private static final Logger logger = LoggerFactory.getLogger(SimulationController.class);

    @Autowired
    private ClockService clockService;

    /**
     * Sets the internal clock to the provided GMT time.
     *
     * @param gmtTime The GMT time to apply.
     */
    @PostMapping("/clock/set")
    public void setTime(@RequestBody final OffsetDateTime gmtTime) {
        logger.info("Received request to set simulation clock to: {}...", gmtTime);
        this.clockService.setTime(gmtTime);
    }
}
