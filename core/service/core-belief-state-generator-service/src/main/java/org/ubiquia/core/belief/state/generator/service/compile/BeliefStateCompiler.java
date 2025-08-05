package org.ubiquia.core.belief.state.generator.service.compile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BeliefStateCompiler {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateCompiler.class);

    public void compileGeneratedSources(
        final String sourceDirPath,
        final String outputDirPath,
        final List<String> dependencyJarPathsOrDirs) throws Exception {

        logger.info("Compiling with input path: \n{} output path: \n{} dependencies: \n{}",
            sourceDirPath,
            outputDirPath,
            dependencyJarPathsOrDirs);

        var compiler = ToolProvider.getSystemJavaCompiler();
        if (Objects.isNull(compiler)) {
            throw new IllegalStateException("JDK required. System compiler not found.");
        }

        var fileManager = compiler.getStandardFileManager(null, null, null);

        // Collect all .java files under sourceDir
        var sourceFiles = Files.walk(Paths.get(sourceDirPath))
            .filter(path -> path.toString().endsWith(".java"))
            .map(Path::toFile)
            .collect(Collectors.toList());

        // Collect all JARs (supporting directories and individual JAR paths)
        var allJarPaths = new ArrayList<String>();
        for (String path : dependencyJarPathsOrDirs) {
            Path p = Paths.get(path);
            if (Files.isDirectory(p)) {
                try (var stream = Files.walk(p)) {
                    stream.filter(jar -> jar.toString().endsWith(".jar"))
                        .forEach(jar -> allJarPaths.add(jar.toAbsolutePath().toString()));
                }
            } else if (path.endsWith(".jar")) {
                allJarPaths.add(p.toAbsolutePath().toString());
            }
        }

        // Join into classpath string
        var classpath = String.join(File.pathSeparator, allJarPaths);
        logger.debug("Resolved classpath:\n{}", classpath);

        // Ensure output directory exists
        Files.createDirectories(Paths.get(outputDirPath));

        // Set compiler options
        var options = new ArrayList<String>();
        options.addAll(Arrays.asList("-classpath", classpath));
        options.addAll(Arrays.asList("-d", outputDirPath));

        // Compile
        var compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
        JavaCompiler.CompilationTask task = compiler.getTask(
            null, fileManager, null, options, null, compilationUnits
        );

        var success = task.call();
        fileManager.close();

        if (!success) {
            throw new RuntimeException("Compilation failed.");
        }

        logger.info("...compilation successful.");
    }
}
