package org.ubiquia.core.flow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.library.dao.controller.GenericUbiquiaDaoController;
import org.ubiquia.common.library.implementation.service.mapper.DomainOntologyDtoMapper;
import org.ubiquia.common.model.ubiquia.IngressResponse;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.common.model.ubiquia.entity.DomainOntologyEntity;
import org.ubiquia.core.flow.repository.DomainOntologyRepository;
import org.ubiquia.core.flow.service.command.controller.DomainOntologyDestroyCommand;
import org.ubiquia.core.flow.service.registrar.DomainOntologyRegistrar;

@RestController
@RequestMapping("/ubiquia/flow-service/domain-ontology")
public class DomainOntologyController extends GenericUbiquiaDaoController<
    DomainOntologyEntity,
    DomainOntology> {

    private static final Logger logger = LoggerFactory.getLogger(DomainOntologyController.class);

    @Autowired
    private EntityDao<DomainOntologyEntity> entityDao;

    @Autowired
    private DomainOntologyRegistrar domainOntologyRegistrar;

    @Autowired
    private DomainOntologyRepository domainOntologyRepository;

    @Autowired
    private DomainOntologyDestroyCommand domainOntologyDestroyCommand;

    @Autowired
    private DomainOntologyDtoMapper dtoMapper;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityDao<DomainOntologyEntity> getDataAccessObject() {
        return this.entityDao;
    }

    @Override
    public InterfaceEntityToDtoMapper<
        DomainOntologyEntity,
        DomainOntology> getDataTransferObjectMapper() {
        return this.dtoMapper;
    }

    /**
     * Delete a graph provided its ID.
     *
     * @param id The id of the graph to delete.
     * @return The ID of the deleted graph.
     */
    @DeleteMapping("/delete/{id}")
    @Transactional
    public ResponseEntity<String> delete(@PathVariable("id") final String id) {
        this.getLogger().info("Received request to delete: ID {}", id);
        ResponseEntity<String> response = null;
        var record = this.domainOntologyRepository.findById(id);
        if (record.isEmpty()) {
            response = ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } else {
            response = ResponseEntity.status(HttpStatus.OK).body(record.get().getId());
            this.domainOntologyDestroyCommand.delete(record.get());
        }
        return response;
    }

    @PostMapping("/register/post")
    @Transactional
    public IngressResponse register(@RequestBody @Valid final DomainOntology domainOntology)
        throws Exception {
        this.getLogger().info("Received a registration request: {}",
            this.objectMapper.writeValueAsString(domainOntology));
        var registeredOntology = this.domainOntologyRegistrar.tryRegister(domainOntology);
        var response = super.ingressResponseBuilder.buildIngressResponseFrom(registeredOntology);
        return response;
    }

    @PostMapping(value = "/register/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IngressResponse upload(@RequestParam("file") MultipartFile file) {
        this.getLogger().info("Received uploaded ontology; processing...");

        IngressResponse response = null;

        try {
            var payload = IOUtils.toString(file.getInputStream(), StandardCharsets.UTF_8);
            var yamlMapper = new ObjectMapper(new YAMLFactory());
            var domainOntology = yamlMapper.readValue(payload, DomainOntology.class);
            var registeredOntology = this.domainOntologyRegistrar.tryRegister(domainOntology);
            response = super.ingressResponseBuilder.buildIngressResponseFrom(registeredOntology);
        } catch (Exception e) {
            throw new IllegalArgumentException("ERROR: Could not transform file into ontology: "
                + e.getMessage());
        }

        return response;
    }
}
