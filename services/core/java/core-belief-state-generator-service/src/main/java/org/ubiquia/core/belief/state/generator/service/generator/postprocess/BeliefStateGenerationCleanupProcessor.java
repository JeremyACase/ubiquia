package org.ubiquia.core.belief.state.generator.service.generator.postprocess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BeliefStateGenerationCleanupProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateGenerationCleanupProcessor.class);

    private final List<String> BLACKLISTED_FILENAMES = List.of(
        "AbstractDomainModel.java",
        "AbstractDomainModelEntity.java",
        "AbstractDomainModelEntityRelationshipBuilder.java",
        "AbstractDomainModelEntityRepository.java",
        "AbstractDomainModelController.java",
        "AbstractDomainModelIngressDtoMapper.java",
        "AbstractDomainModelEgressDtoMapper.java",
        "KeyValuePair.java",
        "KeyValuePairEntity.java",
        "KeyValuePairController.java",
        "KeyValuePairEntityRepository.java",
        "KeyValuePairEntityRelationshipBuilder.java",
        "KeyValuePairIngressDtoMapper.java",
        "KeyValuePairEgressDtoMapper.java"
    );

    public void removeBlacklistedFiles(final Path generatedDir) throws IOException {
        Files.walk(generatedDir)
            .filter(Files::isRegularFile)
            .filter(path -> this.BLACKLISTED_FILENAMES.contains(path.getFileName().toString()))
            .forEach(path -> {
                try {
                    Files.delete(path);
                    logger.info("...deleting blacklisted file: {}", path);
                } catch (IOException e) {
                    logger.warn("ERROR: Failed to delete {}: {}!", path, e.getMessage());
                }
            });
    }
}