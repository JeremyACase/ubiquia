package org.ubiquia.common.library.belief.state.libraries.service.io;


import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.ubiquia.common.library.api.config.UbiquiaAgentConfig;
import org.ubiquia.common.library.api.repository.UbiquiaAgentRepository;
import org.ubiquia.common.library.belief.state.libraries.repository.ObjectMetadataEntityRepository;
import org.ubiquia.common.library.implementation.service.mapper.ObjectMetadataDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.ObjectMetadata;
import org.ubiquia.common.model.ubiquia.entity.ObjectMetadataEntity;
import org.ubiquia.common.model.ubiquia.entity.UbiquiaAgentEntity;

/**
 * A service meant to store relational object metadata in a relational database for any
 * Ubiquia objects stored in object storages.
 */
@Service
public class ObjectMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(ObjectMetadataService.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ObjectMetadataEntityRepository objectMetadataEntityRepository;

    @Autowired
    private ObjectMetadataDtoMapper objectMetadataDtoMapper;

    @Autowired
    private UbiquiaAgentRepository ubiquiaAgentRepository;

    @Autowired
    private UbiquiaAgentConfig ubiquiaAgentConfig;

    /**
     * Persist the object metadata into the relational database.
     *
     * @param bucketName The object bucket where the file is going to be persisted.
     * @param file       The file upload.
     * @return Metadata about the file.
     * @throws JsonProcessingException Exceptions from parsing JSON.
     * @throws SQLException            Exceptions from SQL database.
     */
    @Transactional
    public ObjectMetadata persistObjectMetadata(
        final String bucketName,
        final MultipartFile file)
        throws JsonProcessingException, SQLException {

        logger.info("Received a request to persist object data for "
                + "\nFilename: {}"
                + "\nBucket Name: {}",
            file.getName(),
            bucketName);

        var agent = this.getUbiquiaAgent();
        var objectMetadataEntity = this.buildMetadataEntityFrom(file, bucketName, agent);
        logger.info("...persisted entity with id: {}", objectMetadataEntity.getId());
        var dto = this.objectMetadataDtoMapper.map(objectMetadataEntity);
        return dto;
    }

    /**
     * Helper method to build metadata from a file.
     *
     * @param file       The file to build metadata from.
     * @param bucketName The bucket where the file will be uploaded.
     * @param agent      The Ubiquia agent that will be housing the data.
     * @return Persisted metadata.
     */
    @Transactional
    private ObjectMetadataEntity buildMetadataEntityFrom(
        final MultipartFile file,
        final String bucketName,
        final UbiquiaAgentEntity agent) {

        var metadataEntity = new ObjectMetadataEntity();
        metadataEntity.setBucketName(bucketName);
        metadataEntity.setSize(file.getSize());
        metadataEntity.setContentType(file.getContentType());
        metadataEntity.setOriginalFilename(file.getOriginalFilename());
        metadataEntity.setUbiquiaAgent(agent);
        metadataEntity = this.objectMetadataEntityRepository.save(metadataEntity);

        return metadataEntity;
    }

    /**
     * Fetch this Ubiquia agent information from the relational database.
     *
     * @return The database record for this Ubiquia agent.
     * @throws SQLException Exceptions from database stuff.
     */
    @Transactional
    private UbiquiaAgentEntity getUbiquiaAgent() throws SQLException {
        var agentRecord = this.ubiquiaAgentRepository
            .findById(this.ubiquiaAgentConfig.getId());

        if (agentRecord.isEmpty()) {
            logger.error("ERROR: Could not find agent with ID: {}",
                this.ubiquiaAgentConfig.getId());

            if (this.isRunningH2()) {
                this.initializeInMemoryDatabaseAgent();
                agentRecord = this.ubiquiaAgentRepository.findById(
                    this.ubiquiaAgentConfig.getId());
            }
        }
        var agent = agentRecord.get();

        logger.debug("...persisting metadata against agent id '{}'...",
            this.ubiquiaAgentConfig.getId());
        return agent;
    }

    /**
     * Determine whether or not we're running in H2.
     *
     * @return Whether or not this Ubiquia agent is running with an H2 database.
     * @throws SQLException Exception from SQL stuff.
     */
    @Transactional
    private Boolean isRunningH2() throws SQLException {

        var isRunningH2 = false;
        logger.info("...determining what database service is running against...");
        try (var conn = dataSource.getConnection()) {
            var metaData = conn.getMetaData();
            var url = metaData.getURL();
            var dbName = metaData.getDatabaseProductName();

            logger.info("...service is running against: {} at URL {}",
                dbName,
                url);

            if (url.contains("h2")) {
                isRunningH2 = true;
            }
        }
        return isRunningH2;
    }

    /**
     * Initialize our H2 database with the Ubiquia information since we cannot get it from an
     * external database.
     */
    @Transactional
    public void initializeInMemoryDatabaseAgent() {
        logger.info("...service is running a local in-memory database; " +
                "initializing inter agent id: {}...",
            this.ubiquiaAgentConfig.getId());

        var entity = new UbiquiaAgentEntity();
        entity.setDeployedGraphs(new ArrayList<>());
        entity.setId(this.ubiquiaAgentConfig.getId());
        entity = this.ubiquiaAgentRepository.save(entity);
        logger.info("...created ubiquia agent entity with id: {}", entity.getId());

        logger.info("...finished initialization.");
    }
}
