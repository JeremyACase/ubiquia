package org.ubiquia.core.belief.state.generator.service.generator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguage;

@Service
public class GenerationSupportProcessor {

    @Value("${ubiquia.agent.storage.minio.enabled}")
    private Boolean minioEnabled;

    public void postProcess(final AgentCommunicationLanguage acl) throws IOException {
        this.copyResourceFromClasspath(
            "template/java/support/Application.java.template",
            "generated/src/main/java/org/ubiquia/acl/generated/Application.java");

        this.copyResourceFromClasspath(
            "template/java/support/GlobalExceptionHandler.java.template",
            "generated/src/main/java/org/ubiquia/acl/generated/GlobalExceptionHandler.java");

        this.copyResourceFromClasspath(
            "template/java/support/ObjectController.java.template",
            "generated/src/main/java/org/ubiquia/acl/generated/ObjectController.java");

        var tokenMap = new HashMap<String, String>();
        tokenMap.put("{DOMAIN_NAME}", acl.getDomain());
        tokenMap.put("{MINIO_ENABLED}", this.minioEnabled.toString());
        this.copyAndReplacePlaceholders(
            "template/java/support/application.yaml.template",
            "generated/src/main/resources/application.yaml",
            tokenMap);
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

    private void copyAndReplacePlaceholders(
        final String resourcePath,
        final String destinationPath,
        final Map<String, String> replacements)
        throws IOException {

        try (var in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found in classpath: " + resourcePath);
            }

            var content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            for (var entry : replacements.entrySet()) {
                var token = entry.getKey();
                var value = entry.getValue();
                content = content.replace(token, value);
            }

            var targetPath = Paths.get(destinationPath);
            Files.createDirectories(targetPath.getParent());
            Files.writeString(targetPath, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        }
    }
}

