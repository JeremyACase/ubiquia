package org.ubiquia.core.belief.state.generator.service.generator.preprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;

@Component
public class CleanupPreprocessor {

    private static final Logger logger = LoggerFactory.getLogger(CleanupPreprocessor.class);

    @Value("${ubiquia.beliefStateGeneratorService.directories.generated:generated}")
    private String generatedDir;

    @Value("${ubiquia.beliefStateGeneratorService.directories.compiled:compiled}")
    private String compiledDir;

    @Value("${ubiquia.beliefStateGeneratorService.directories.packaged:packaged}")
    private String packagedDir;

    /**
     * Deletes all regular files (recursively) under the configured directories.
     * Keeps the directory structure intact. Creates the directories if missing.
     */
    public void preprocess() throws IOException {
        var base = Paths.get("").toAbsolutePath();
        cleanDirectory(base.resolve(generatedDir));
        cleanDirectory(base.resolve(compiledDir));
        cleanDirectory(base.resolve(packagedDir));
    }

    private void cleanDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            logger.info("Directory does not exist; creating: {}", dir.toAbsolutePath());
            Files.createDirectories(dir);
            return;
        }
        if (!Files.isDirectory(dir)) {
            logger.warn("Path is not a directory; skipping: {}", dir.toAbsolutePath());
            return;
        }

        logger.info("Cleaning files under: {}", dir.toAbsolutePath());

        // Delete only files; keep directories (including nested), so rebuilds don't need to recreate structure
        try (var walk = Files.walk(dir)) {
            walk
                .sorted(Comparator.reverseOrder()) // safe order if you later toggle folder deletion
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                        logger.debug("Deleted file: {}", path.toAbsolutePath());
                    } catch (IOException e) {
                        logger.warn("Failed to delete file: {}", path.toAbsolutePath(), e);
                    }
                });
        }
    }
}
