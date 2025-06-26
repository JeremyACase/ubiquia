package org.ubiquia.core.belief.state.generator.service.packager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BeliefStateUberizer {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateUberizer.class);

    public void createUberJar(
        String outputJarPath,
        String compiledClassDir,
        List<String> dependencyJarPaths)
        throws IOException {

        var manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "org.ubiquia.acl.generated.Application");

        Set<String> addedEntries = new HashSet<>();

        try (FileOutputStream fos = new FileOutputStream(outputJarPath);
             JarOutputStream jos = new JarOutputStream(fos, manifest)) {

            // Add compiled class files
            addDirectoryToJar(Paths.get(compiledClassDir), jos, compiledClassDir.length() + 1, addedEntries);

            // Add classes from dependency JARs
            for (String depJarPath : dependencyJarPaths) {
                try (JarInputStream jarIn = new JarInputStream(new FileInputStream(depJarPath))) {
                    JarEntry entry;
                    while ((entry = jarIn.getNextJarEntry()) != null) {
                        String name = entry.getName();
                        if (entry.isDirectory()
                            || name.startsWith("META-INF/")
                            || name.equals("module-info.class")
                            || !addedEntries.add(name)) {
                            continue; // skip duplicates
                        }

                        jos.putNextEntry(new JarEntry(name));
                        jarIn.transferTo(jos);
                        jos.closeEntry();
                    }
                }
            }
        }
    }

    private void addDirectoryToJar(Path sourceDir, JarOutputStream jos, int prefixLength, Set<String> addedEntries) throws IOException {
        Files.walk(sourceDir).filter(Files::isRegularFile).forEach(file -> {
            try (InputStream in = Files.newInputStream(file)) {
                String entryName = file.toString().substring(prefixLength).replace("\\", "/");
                if (!addedEntries.add(entryName)) {
                    return;
                }

                jos.putNextEntry(new ZipEntry(entryName));
                in.transferTo(jos);
                jos.closeEntry();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
