package org.ubiquia.core.flow.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.ubiquia.common.library.api.controller.GenericUbiquiaDaoController;
import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.model.ubiquia.IngressResponse;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguage;
import org.ubiquia.common.model.ubiquia.entity.AgentCommunicationLanguageEntity;
import org.ubiquia.core.flow.repository.AdapterRepository;
import org.ubiquia.core.flow.repository.AgentCommunicationLanguageRepository;
import org.ubiquia.core.flow.repository.AgentRepository;
import org.ubiquia.core.flow.repository.GraphRepository;
import org.ubiquia.core.flow.service.mapper.AgentCommunicationLanguageDtoMapper;
import org.ubiquia.core.flow.service.registrar.AgentCommunicationLanguageRegistrar;

/**
 * A controller that exposes a RESTful interface for Agent Communication Language.
 */
@RestController
@RequestMapping("/agent-communication-language")
public class AgentCommunicationLanguageController extends GenericUbiquiaDaoController<
    AgentCommunicationLanguageEntity,
    AgentCommunicationLanguage> {

    private static final Logger logger = LoggerFactory.getLogger(
        AgentCommunicationLanguageController.class);

    @Autowired
    private EntityDao<AgentCommunicationLanguageEntity> entityDao;

    @Autowired
    private AgentCommunicationLanguageDtoMapper dtoMapper;

    @Autowired
    private AdapterRepository adapterRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private AgentCommunicationLanguageRepository aclRepository;

    @Autowired
    private AgentCommunicationLanguageRegistrar aclRegistrar;

    @Autowired
    private GraphRepository graphRepository;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityDao<AgentCommunicationLanguageEntity> getDataAccessObject() {
        return this.entityDao;
    }

    @Override
    public InterfaceEntityToDtoMapper<
        AgentCommunicationLanguageEntity,
        AgentCommunicationLanguage> getDataTransferObjectMapper() {
        return this.dtoMapper;
    }

    /**
     * Delete the domain ontology with the provided ID.
     *
     * @param id The ID of the domain to delete.
     * @return A response with the ID to denote successful deletion.
     */
    @DeleteMapping("/delete/{id}")
    @Transactional
    public ResponseEntity<String> delete(@PathVariable("id") final String id) {
        logger.info("Received request to delete: ID {}", id);
        ResponseEntity<String> response = null;
        var record = this.aclRepository.findById(id);
        if (record.isEmpty()) {
            response = ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } else {
            response = ResponseEntity.status(HttpStatus.OK).body(record.get().getId());
            for (var graph : record.get().getGraphs()) {
                this.adapterRepository.deleteAll(graph.getAdapters());
                this.agentRepository.deleteAll(graph.getAgents());
                this.graphRepository.delete(graph);
            }
            this.aclRepository.delete(record.get());
        }
        return response;
    }

    @PostMapping("/register/post")
    @Transactional
    public IngressResponse register(
        @RequestBody @Valid final AgentCommunicationLanguage communicationLanguage)
        throws JsonProcessingException {
        logger.info("Received a registration request: {}",
            super.objectMapper.writeValueAsString(communicationLanguage));
        var response = this.tryRegisterAgentCommunicationLanguage(communicationLanguage);
        return response;
    }

    /**
     * Upload a multi-part file representing a domain ontology.
     *
     * @param file The file to upload.
     * @return A response with some data about the ontology.
     */
    @PostMapping(value = "/register/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IngressResponse upload(@RequestParam("file") MultipartFile file) {
        logger.info("Received uploaded domain ontology; processing...");

        IngressResponse response = null;
        try {
            var payload = IOUtils.toString(file.getInputStream(), "UTF-8");
            var acl = super.objectMapper.readValue(
                payload,
                AgentCommunicationLanguage.class);
            response = this.tryRegisterAgentCommunicationLanguage(acl);
        } catch (Exception e) {
            throw new IllegalArgumentException("ERROR: Could not transform file into agent "
                + "communication language: "
                + e.getMessage());
        }

        return response;
    }

    /**
     * Attempt to register the provided DTO object representing a domain ontology.
     *
     * @param agentCommunicationLanguage The object to try to register.
     * @return A response with some data about the registration.
     * @throws JsonProcessingException Exceptions from parsing the domain ontology.
     */
    private IngressResponse tryRegisterAgentCommunicationLanguage(
        final AgentCommunicationLanguage agentCommunicationLanguage)
        throws JsonProcessingException {

        var entity = this.aclRegistrar.tryRegister(agentCommunicationLanguage);
        var response = super.ingressResponseBuilder.buildIngressResponseFrom(entity);
        return response;
    }
}
