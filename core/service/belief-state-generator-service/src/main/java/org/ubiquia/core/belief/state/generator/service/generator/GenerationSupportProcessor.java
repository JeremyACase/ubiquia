package org.ubiquia.core.belief.state.generator.service.generator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GenerationSupportProcessor {

    public void postProcess() throws IOException {
        this.copyResourceFromClasspath(
            "template/java/support/Application.java.template",
            "generated/src/main/java/org/ubiquia/acl/generated/Application.java");

        this.copyResourceFromClasspath(
            "template/java/support/GlobalExceptionHandler.java.template",
            "generated/src/main/java/org/ubiquia/acl/generated/GlobalExceptionHandler.java");

        this.copyResourceFromClasspath(
            "template/java/support/application.yaml.template",
            "generated/src/main/resources/application.yaml");
    }

    private void copyResourceFromClasspath(String resourcePath, String destinationPath) throws IOException {
        try (var in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found in classpath: "
                    + resourcePath);
            }
            Files.createDirectories(Paths.get(destinationPath).getParent());
            Files.copy(in, Paths.get(destinationPath), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

