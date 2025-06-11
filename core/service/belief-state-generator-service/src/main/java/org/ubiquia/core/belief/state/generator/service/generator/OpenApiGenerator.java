package org.ubiquia.core.belief.state.generator.service.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OpenApiGenerator {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiGenerator.class);

    @Autowired
    private ObjectMapper objectMapper;

    public void generateOpenApiModelsFrom(final String openApiYaml) {

        logger.debug("Generating new Belief State from: {}", openApiYaml);

        Path tempPath;
        try {
            tempPath = Files.createTempFile("openapi-spec-", ".yaml");
            Files.writeString(tempPath, openApiYaml, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write OpenAPI spec to temp file", e);
        }
        var specFilePath = tempPath.toAbsolutePath().toString();

        var configurator = new CodegenConfigurator()
            .setInputSpec(specFilePath)
            .setGeneratorName("java")
            .setOutputDir("build/generated/test")
            //.setTemplateDir("/absolute/path/to/custom/templates")
            .addAdditionalProperty("modelPackage", "org.ubiquia.acl.generated");

        var clientOptInput = configurator.toClientOptInput();
        var generator = new DefaultGenerator();
        generator.opts(clientOptInput).generate();
    }
}
