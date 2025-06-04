package org.ubiquia.core.flow.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.ubiquia.common.library.api.controller.GenericUbiquiaDaoController;
import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.model.ubiquia.IngressResponse;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguageDto;
import org.ubiquia.common.model.ubiquia.dto.AgentDto;
import org.ubiquia.common.model.ubiquia.dto.UbiquiaAgentDto;
import org.ubiquia.common.model.ubiquia.entity.Agent;
import org.ubiquia.common.model.ubiquia.entity.UbiquiaAgent;
import org.ubiquia.core.flow.config.UbiquiaAgentConfig;
import org.ubiquia.core.flow.repository.UbiquiaAgentRepository;
import org.ubiquia.core.flow.service.mapper.AgentDtoMapper;
import org.ubiquia.core.flow.service.mapper.UbiquiaAgentDtoMapper;

/**
 * A controller that exposes a RESTful interface for agents.
 */
@RestController
@RequestMapping("/ubiquia/ubiquia-agent")
public class UbiquiaAgentController implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(UbiquiaAgentController.class);

    @Autowired
    private UbiquiaAgentDtoMapper dtoMapper;

    @Autowired
    private UbiquiaAgentConfig ubiquiaAgentConfig;

    @Autowired
    private UbiquiaAgentRepository ubiquiaAgentRepository;

    @Autowired
    private EntityDao<UbiquiaAgent> entityDao;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @GetMapping(value = "/instance/get")
    @Transactional
    public UbiquiaAgentDto getInstance() throws JsonProcessingException {
        logger.info("Received request for Ubiquia instance...");
        var id = this.ubiquiaAgentConfig.getId();
        var record = this.ubiquiaAgentRepository.findById(id);
        if (record.isEmpty()) {
            throw new RuntimeException("ERROR: Could not find instance id in database: "
            + id);
        }
        return this.dtoMapper.map(record.get());
    }
}
