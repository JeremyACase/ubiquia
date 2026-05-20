package org.ubiquia.common.library.belief.state.libraries.service.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.ubiquia.common.library.belief.state.libraries.entity.ObjectMetadataEntity;
import org.ubiquia.common.library.belief.state.libraries.repository.ObjectMetadataEntityRepository;
import org.ubiquia.common.library.belief.state.libraries.service.mapper.ObjectMetadataEgressDtoMapper;
import org.ubiquia.common.model.domain.dto.ObjectMetadataDto;

@ConditionalOnProperty(value = "ubiquia.agent.storage.minio.enabled", havingValue = "true")
@Service
public class ObjectMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(ObjectMetadataService.class);

    @Autowired
    private ObjectMetadataEntityRepository objectMetadataEntityRepository;

    @Autowired
    private ObjectMetadataEgressDtoMapper objectMetadataEgressDtoMapper;

    @Transactional
    public ObjectMetadataDto persistObjectMetadata(
        final String bucketName,
        final MultipartFile file)
        throws JsonProcessingException {

        logger.info("Received a request to persist object data for "
                + "\nFilename: {}"
                + "\nBucket Name: {}",
            file.getName(),
            bucketName);

        var entity = new ObjectMetadataEntity();
        entity.setBucketName(bucketName);
        entity.setSize(file.getSize());
        entity.setContentType(file.getContentType());
        entity.setOriginalFilename(file.getOriginalFilename());
        entity = this.objectMetadataEntityRepository.save(entity);

        logger.info("...persisted entity with id: {}", entity.getUbiquiaId());
        try {
            return (ObjectMetadataDto) this.objectMetadataEgressDtoMapper.map(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to map ObjectMetadataEntity to DTO", e);
        }
    }
}
