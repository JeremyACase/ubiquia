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

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK required. System compiler not found.");

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        // Collect all .java files under sourceDir
        List<File> sourceFiles = Files.walk(Paths.get(sourceDirPath))
            .filter(path -> path.toString().endsWith(".java"))
            .map(Path::toFile)
            .collect(Collectors.toList());

        // Prepare classpath
        String classpath = String.join(File.pathSeparator, dependencyJars);

        // Create output directory
        Files.createDirectories(Paths.get(outputDirPath));

        // Set compiler options
        List<String> options = new ArrayList<>();
        options.addAll(Arrays.asList("-classpath", classpath));
        options.addAll(Arrays.asList("-d", outputDirPath));

        // Compile
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
        JavaCompiler.CompilationTask task = compiler.getTask(
            null, fileManager, null, options, null, compilationUnits
        );

        boolean success = task.call();
        fileManager.close();

        if (!success) throw new RuntimeException("Compilation failed.");
        System.out.println("Compilation successful!");
    }
}
