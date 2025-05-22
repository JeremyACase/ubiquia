package org.ubiquia.core.flow.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.models.dto.AdapterDto;
import org.ubiquia.common.models.entity.Adapter;

/**
 * A controller that exposes a RESTful interface for adapter.
 */
@RestController
@RequestMapping("/ubiquia/adapter")
public class AdapterController extends GenericEntityController<Adapter, AdapterDto> {

    private static final Logger logger = LoggerFactory.getLogger(AdapterController.class);

    @Override
    public Logger getLogger() {
        return logger;
    }
}
