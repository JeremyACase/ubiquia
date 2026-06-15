package org.ubiquia.core.belief.state.generator.service.generator.postprocess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Post-processor that removes generated files which conflict with shared library classes. */
@Service
public class BeliefStateGenerationCleanupProcessor {

    private static final Logger logger =
        LoggerFactory.getLogger(BeliefStateGenerationCleanupProcessor.class);

    // AbstractDomainModel* files are deleted because the shared library (domain-*.jar) already
    // provides AbstractDomainModel and AbstractDomainModelEntity; generated classes extend those
    // via the wildcard imports in the model templates. KeyValuePair and KeyValuePairEntity are
    // NOT blacklisted — generated models import them from org.ubiquia.domain.generated and they
    // must remain on the classpath. The generators' postProcessFile handles deletion of the
    // embeddable support files (Controller, Repository, RelationshipBuilder, DtoMappers).
    private final List<String> blacklistedFilenames = List.of(
        "AbstractDomainModel.java",
        "AbstractDomainModelEntity.java",
        "AbstractDomainModelEntityRelationshipBuilder.java",
        "AbstractDomainModelEntityRepository.java",
        "AbstractDomainModelController.java",
        "AbstractDomainModelIngressDtoMapper.java",
        "AbstractDomainModelEgressDtoMapper.java"
    );

    /** Deletes all generated files whose names appear in the blacklist. */
    public void removeBlacklistedFiles(final Path generatedDir) throws IOException {
        Files.walk(generatedDir)
            .filter(Files::isRegularFile)
            .filter(path -> this.blacklistedFilenames.contains(path.getFileName().toString()))
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