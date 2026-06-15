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

/** Compiles generated Java sources using the system Java compiler. */
@Service
public class BeliefStateCompiler {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateCompiler.class);

    /** Compiles all Java source files under {@code sourceDirPath} to {@code outputDirPath}. */
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

        final var fileManager = compiler.getStandardFileManager(null, null, null);

        // Collect all .java files under sourceDir
        final var sourceFiles = Files.walk(Paths.get(sourceDirPath))
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

        // Collect diagnostics so failures are visible in logs
        var diagnostics = new javax.tools.DiagnosticCollector<javax.tools.JavaFileObject>();
        var compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
        JavaCompiler.CompilationTask task = compiler.getTask(
            null, fileManager, diagnostics, options, null, compilationUnits
        );

        var success = task.call();
        fileManager.close();

        if (!success) {
            for (var d : diagnostics.getDiagnostics()) {
                if (d.getKind() == javax.tools.Diagnostic.Kind.ERROR) {
                    logger.error("Compile error [{}:{}] {}",
                        d.getSource() != null ? d.getSource().getName() : "<unknown>",
                        d.getLineNumber(),
                        d.getMessage(java.util.Locale.ENGLISH));
                }
            }
            throw new RuntimeException("Compilation failed — see ERROR lines above.");
        }

        logger.info("...compilation successful.");
    }
}
