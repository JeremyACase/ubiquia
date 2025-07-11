package org.ubiquia.core.belief.state.generator.service.generator.openapi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OpenApiDtoGenerator {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiDtoGenerator.class);

    @Value("${ubiquia.beliefStateGeneratorService.template.filepath.dto}")
    private String templateFilepathDto;

    public void generateOpenApiDtosFrom(final String openApiYaml) {

        logger.debug("Generating new Belief State from: {}", openApiYaml);

        Path tempPath;
        try {
            tempPath = Files.createTempFile("openapi-spec-dto", ".yaml");
            Files.writeString(tempPath, openApiYaml, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write OpenAPI spec to temp file", e);
        }
        var specFilePath = tempPath.toAbsolutePath().toString();

        var templatePath = Paths.get(this.templateFilepathDto)
            .toAbsolutePath()
            .toString();

        var configurator = new CodegenConfigurator()
            .setInputSpec(specFilePath)
            .setGeneratorName("ubiquia-acl-dto-generator")
            .setOutputDir("generated")
            .setTemplateDir(templatePath)
            .addAdditionalProperty("useBeanValidation", true)
            .addAdditionalProperty("gson", false)
            .addAdditionalProperty("jackson", false)
            .addAdditionalProperty("library", "native")
            .addAdditionalProperty("modelPropertyNaming", "original")
            .addAdditionalProperty("javaxPackage", "jakarta")
            .addAdditionalProperty("modelPackage", "org.ubiquia.acl.generated");

        configurator.setGlobalProperties(Map.of(
            "models", "",
            "modelDocs", "false",
            "modelTests", "false",
            "apis", "false",
            "apiDocs", "false",
            "apiTests", "false"
        ));

        var clientOptInput = configurator.toClientOptInput();
        var generator = new DefaultGenerator();
        generator.opts(clientOptInput).generate();
    }
}
