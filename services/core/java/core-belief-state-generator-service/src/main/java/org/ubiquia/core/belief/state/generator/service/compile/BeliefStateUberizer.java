package org.ubiquia.core.belief.state.generator.service.compile;

import java.io.*;
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
     * @param outputJarPath      Path to final .jar file (e.g. build/packaged/pets-1.2.3.jar)
     * @param compiledClassDir   Directory with .class files
     * @param dependencyJarPaths List of paths to dependency JARs
     * @throws IOException if file IO fails
     */
    public void createUberJar(
        final String outputJarPath,
        final String compiledClassDir,
        final List<String> dependencyJarPaths
    ) throws IOException {

        // Ensure output directory exists
        Path outputPath = Paths.get(outputJarPath);
        Files.createDirectories(outputPath.getParent());

        Path classesPath = Paths.get(compiledClassDir);
        if (!Files.exists(classesPath)) {
            throw new FileNotFoundException("Compiled class directory not found: "
                + classesPath.toAbsolutePath());
        }

        Path resourcesPath = Paths.get("generated/src/main/resources"); // adjust if necessary

        // Create temporary base JAR
        Path baseJarPath = Files.createTempFile("base", ".jar");

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
                        logger.debug("Added class: {}", entryName);
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
                            String entryName = resourcesPath.relativize(path).toString().replace("\\", "/");
                            jos.putNextEntry(new ZipEntry(entryName));
                            Files.copy(path, jos);
                            jos.closeEntry();
                            logger.debug("Added resource: {}", entryName);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
            } else {
                logger.warn("Resource directory not found: {}", resourcesPath.toAbsolutePath());
            }
        }

        // Repackage into Spring Boot format
        Repackager repackager = new Repackager(baseJarPath.toFile());
        repackager.setMainClass("org.ubiquia.domain.generated.Application"); // Update if needed
        repackager.setLayout(new Layouts.Jar());

        repackager.repackage(
            outputPath.toFile(),
            (callback) -> {
                for (String depPath : dependencyJarPaths) {
                    File f = new File(depPath);
                    if (f.exists() && f.getName().endsWith(".jar")) {
                        callback.library(new Library(f, LibraryScope.COMPILE));
                        logger.debug("Included dependency: {}", f.getAbsolutePath());
                    } else {
                        logger.warn("Dependency not found or not a jar: {}", depPath);
                    }
                }
            }
        );

        logger.info("Created Spring Boot executable jar: {}", outputJarPath);
    }
}
