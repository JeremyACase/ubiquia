package org.ubiquia.core.belief.state.generator.service.generator.postprocess;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;

@Service
public class BeliefStateGenerationSupportProcessor {

    @Value("${ubiquia.agent.storage.minio.enabled:false}")
    private Boolean minioEnabled;

    @Autowired
    private AgentConfig agentConfig;

    public void postProcess(final DomainOntology domainOntology) throws IOException {
        this.copyResourceFromClasspath(
            "template/java/support/Application.java.template",
            "generated/src/main/java/org/ubiquiadomainl/generated/Application.java");

        this.copyResourceFromClasspath(
            "template/java/support/GlobalExceptionHandler.java.template",
            "generated/src/main/java/org/ubiquia/domain/generated/GlobalExceptionHandler.java");

        this.copyResourceFromClasspath(
            "template/java/support/MinioClientConfig.java.template",
            "generated/src/main/java/org/ubiquia/domain/generated/MinioClientConfig.java");

        this.copyResourceFromClasspath(
            "template/java/support/ObjectController.java.template",
            "generated/src/main/java/org/ubiquia/domain/generated/ObjectController.java");

        var tokenMap = new HashMap<String, String>();
        tokenMap.put("{DOMAIN_NAME}", domainOntology.getName());
        tokenMap.put("{MINIO_ENABLED}", String.valueOf(this.minioEnabled));
        tokenMap.put("{UBIQUIA_AGENT_ID}", this.agentConfig.getId());

        tokenMap.putAll(this.resolveDbTokens());

        this.copyAndReplacePlaceholders(
            "template/java/support/application.yaml.template",
            "generated/src/main/resources/application.yaml",
            tokenMap);
    }

    private Map<String, String> resolveDbTokens() {
        var tokens = new HashMap<String, String>();
        tokens.put("{DB_DRIVER_CLASS}", "org.h2.Driver");
        tokens.put("{DB_URL}", "jdbc:h2:mem:myDb;DB_CLOSE_DELAY=-1");
        tokens.put("{DB_USERNAME}", "sa");
        tokens.put("{DB_PASSWORD}", "sa");
        return tokens;
    }

    private void copyResourceFromClasspath(String resourcePath, String destinationPath) throws IOException {
        try (var in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (Objects.isNull(in)) {
                throw new FileNotFoundException("Resource not found in classpath: " + resourcePath);
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
            if (Objects.isNull(in)) {
                throw new FileNotFoundException("Resource not found in classpath: " + resourcePath);
            }

            var content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (var entry : replacements.entrySet()) {
                content = content.replace(entry.getKey(), entry.getValue());
            }

            var targetPath = Paths.get(destinationPath);
            Files.createDirectories(targetPath.getParent());
            Files.writeString(
                targetPath, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
}
