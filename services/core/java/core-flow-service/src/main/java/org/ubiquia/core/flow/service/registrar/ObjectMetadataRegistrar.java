package org.ubiquia.core.flow.service.registrar;

import jakarta.transaction.Transactional;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.common.model.ubiquia.dto.ObjectMetadata;
import org.ubiquia.common.model.ubiquia.entity.ObjectMetadataEntity;
import org.ubiquia.core.flow.repository.ObjectMetadataRepository;

@Service
public class ObjectMetadataRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(ObjectMetadataRegistrar.class);

    @Autowired
    private ObjectMetadataRepository objectMetadataRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Transactional
    public void tryRegister(final ObjectMetadata dto) {
        if (Objects.nonNull(dto.getId()) && this.objectMetadataRepository.existsById(dto.getId())) {
            logger.debug("ObjectMetadata {} already exists; skipping.", dto.getId());
            return;
        }

        if (Objects.isNull(dto.getUbiquiaAgent()) || Objects.isNull(dto.getUbiquiaAgent().getId())) {
            throw new IllegalArgumentException(
                "Cannot register ObjectMetadata: missing agent reference.");
        }
        var agentOpt = this.agentRepository.findById(dto.getUbiquiaAgent().getId());
        if (agentOpt.isEmpty()) {
            throw new IllegalArgumentException(
                "Cannot register ObjectMetadata: Agent " + dto.getUbiquiaAgent().getId()
                    + " not found.");
        }

        var entity = new ObjectMetadataEntity();
        if (Objects.nonNull(dto.getId())) {
            entity.setId(dto.getId());
        }
        entity.setAgent(agentOpt.get());
        entity.setBucketName(dto.getBucketName());
        entity.setContentType(dto.getContentType());
        entity.setOriginalFilename(dto.getOriginalFilename());
        entity.setSize(dto.getSize());
        this.objectMetadataRepository.save(entity);
        logger.info("Registered ObjectMetadata {}.", entity.getId());
    }
}
