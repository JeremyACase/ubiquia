package org.ubiquia.core.belief.state.generator.service.compile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.loader.tools.Layouts;
import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryScope;
import org.springframework.boot.loader.tools.Repackager;
import org.springframework.stereotype.Service;

@Service
public class BeliefStateUberizer {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateUberizer.class);

    /**
     * Creates a Spring Boot executable Uber JAR.
     *
     * @param outputJarPath      Path to final .jar file
     * @param compiledClassDir   Directory with .class files (e.g., target/classes or compiled output)
     * @param dependencyJarPaths List of paths to dependency JARs
     * @throws IOException if file IO fails
     */
    public void createUberJar(
        String outputJarPath,
        String compiledClassDir,
        List<String> dependencyJarPaths
    ) throws IOException {

        // Create temporary base jar file
        Path baseJarPath = Files.createTempFile("base", ".jar");

        Path classesPath = Paths.get(compiledClassDir);

        // This assumes resources are under generated/src/main/resources
        // You can update this path if you're writing resources elsewhere
        Path resourcesPath = Paths.get("generated/src/main/resources");

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(baseJarPath.toFile()))) {

            // Add compiled classes
            Files.walk(classesPath)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        String entryName = classesPath.relativize(path).toString().replace("\\", "/");
                        jos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(path, jos);
                        jos.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

            // Add resource files (like application.yaml)
            if (Files.exists(resourcesPath)) {
                Files.walk(resourcesPath)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            // Preserve standard resource path inside the JAR
                            String entryName = resourcesPath.relativize(path).toString().replace("\\", "/");
                            jos.putNextEntry(new ZipEntry(entryName));
                            Files.copy(path, jos);
                            jos.closeEntry();
                            logger.debug("Added resource to jar: {}", entryName);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
            } else {
                logger.warn("No resource directory found at {}", resourcesPath.toAbsolutePath());
            }
        }

        // Repackage into Spring Boot format
        Repackager repackager = new Repackager(baseJarPath.toFile());
        repackager.setMainClass("org.ubiquia.acl.generated.Application");
        repackager.setLayout(new Layouts.Jar());

        repackager.repackage(
            new File(outputJarPath),
            (callback) -> {
                for (String depPath : dependencyJarPaths) {
                    File f = new File(depPath);
                    if (f.exists() && f.getName().endsWith(".jar")) {
                        callback.library(new Library(f, LibraryScope.COMPILE));
                    }
                }
            }
        );

        logger.info("Created Spring Boot executable jar: {}", outputJarPath);
    }
}
