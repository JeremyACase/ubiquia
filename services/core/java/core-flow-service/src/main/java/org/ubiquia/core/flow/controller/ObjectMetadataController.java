package org.ubiquia.core.flow.controller;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.library.dao.controller.GenericUbiquiaDaoController;
import org.ubiquia.common.library.implementation.service.mapper.ObjectMetadataDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.ObjectMetadata;
import org.ubiquia.common.model.ubiquia.entity.ObjectMetadataEntity;
import org.ubiquia.core.flow.service.registrar.ObjectMetadataRegistrar;

@RestController
@RequestMapping("/ubiquia/core/flow-service/object-metadata")
public class ObjectMetadataController
    extends GenericUbiquiaDaoController<ObjectMetadataEntity, ObjectMetadata> {

    private static final Logger logger = LoggerFactory.getLogger(ObjectMetadataController.class);

    @Autowired
    private ObjectMetadataDtoMapper dtoMapper;

    @Autowired
    private EntityDao<ObjectMetadataEntity> entityDao;

    @Autowired
    private ObjectMetadataRegistrar objectMetadataRegistrar;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityDao<ObjectMetadataEntity> getDataAccessObject() {
        return this.entityDao;
    }

    @Override
    public InterfaceEntityToDtoMapper<ObjectMetadataEntity, ObjectMetadata>
        getDataTransferObjectMapper() {
        return this.dtoMapper;
    }

    @PostMapping("/register/post")
    @Transactional
    public void register(@RequestBody final ObjectMetadata objectMetadata) {
        this.getLogger().info("Received ObjectMetadata registration request for id {}.",
            objectMetadata.getId());
        this.objectMetadataRegistrar.tryRegister(objectMetadata);
    }
}
