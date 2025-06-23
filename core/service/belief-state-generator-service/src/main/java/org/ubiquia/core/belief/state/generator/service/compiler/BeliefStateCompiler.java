package org.ubiquia.core.belief.state.generator.service.compiler;

import javax.tools.*;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class BeliefStateCompiler {

    public void compileGeneratedSources(
        final String sourceDirPath,
        final String outputDirPath,
        final List<String> dependencyJars) throws Exception {

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

        // Prepare classpath
        var classpath = String.join(File.pathSeparator, dependencyJars);

        // Create output directory
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

        if (!success) throw new RuntimeException("Compilation failed.");
        System.out.println("Compilation successful!");
    }
}
