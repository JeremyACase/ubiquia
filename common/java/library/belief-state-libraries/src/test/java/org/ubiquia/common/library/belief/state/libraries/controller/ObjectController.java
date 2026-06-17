package org.ubiquia.common.library.belief.state.libraries.controller;

import io.minio.errors.MinioException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.ubiquia.common.library.belief.state.libraries.entity.ObjectMetadataEntity;
import org.ubiquia.common.library.belief.state.libraries.repository.ObjectMetadataEntityRepository;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.EntityRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.ObjectMetadataEntityRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.io.MinioService;
import org.ubiquia.common.library.belief.state.libraries.service.io.ObjectMetadataService;
import org.ubiquia.common.library.belief.state.libraries.service.mapper.AbstractIngressDtoMapper;
import org.ubiquia.common.model.domain.dto.ObjectMetadataDto;

/** REST controller for object storage metadata. */
@RestController
@RequestMapping("/ubiquia/belief-state-service/object")
@Transactional
public class ObjectController
    extends AbstractDomainModelController<ObjectMetadataEntity, ObjectMetadataDto> {

    private static final Logger logger = LoggerFactory.getLogger(ObjectController.class);

    @Value("${ubiquia.beliefState.domainName}")
    private String domainName;

    @Autowired(required = false)
    private ObjectMetadataService objectMetadataService;

    @Autowired
    private ObjectMetadataEntityRepository objectMetadataEntityRepository;

    @Autowired
    private ObjectMetadataEntityRelationshipBuilder objectMetadataEntityRelationshipBuilder;

    @Autowired
    private AbstractIngressDtoMapper<ObjectMetadataDto, ObjectMetadataEntity> ingressMapper;

    @Autowired(required = false)
    private MinioService minioService;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityRelationshipBuilder<ObjectMetadataEntity> getEntityRelationshipBuilder() {
        return this.objectMetadataEntityRelationshipBuilder;
    }

    @Override
    public AbstractIngressDtoMapper<ObjectMetadataDto, ObjectMetadataEntity> getIngressMapper() {
        return this.ingressMapper;
    }

    @Override
    public ObjectMetadataEntityRepository getEntityRepository() {
        return this.objectMetadataEntityRepository;
    }

    /** Upload a file to object storage. */
    @PostMapping("/upload")
    public ObjectMetadataDto uploadFile(
        @RequestParam("file") MultipartFile file)
        throws IOException, MinioException {

        this.getLogger().info("Received a request to upload file {} to bucket {}...",
            file.getName(),
            this.domainName);

        if (Objects.isNull(this.minioService)) {
            throw new IllegalArgumentException("ERROR: No available object storage service "
                + "is currently running...");
        }

        var metadata = this.objectMetadataService.persistObjectMetadata(this.domainName, file);
        this.minioService.uploadFile(this.domainName, metadata.getUbiquiaId(), file);
        this.getLogger().info("...object stored: {}",
            this.objectMapper.writeValueAsString(metadata));

        return metadata;
    }

    /** Download a file from object storage. */
    @GetMapping("/download/{filename}")
    public ResponseEntity<byte[]> downloadFile(
        @PathVariable("filename") String filename)
        throws MinioException, IOException {

        this.getLogger().info("Received a request to download file {} from bucket {}...",
            filename,
            this.domainName);

        InputStream data = null;
        try {
            data = this.minioService.downloadFile(this.domainName, filename);
        } catch (MinioException e) {
            logger.info("MinioException: {}", e.getMessage());
        }

        if (Objects.isNull(data)) {
            this.getLogger().info("...no file found in that bucket...");
            return ResponseEntity.notFound().build();
        }

        this.getLogger().debug("...file found; retrieving...");
        return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(data.readAllBytes());
    }
}
