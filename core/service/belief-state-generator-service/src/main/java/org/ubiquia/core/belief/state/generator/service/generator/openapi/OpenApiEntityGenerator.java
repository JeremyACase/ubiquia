package org.ubiquia.core.belief.state.generator.service.generator.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OpenApiEntityGenerator {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiEntityGenerator.class);

    @Autowired
    private ObjectMapper objectMapper;

    public void generateOpenApiEntitiesFrom(final String openApiYaml) throws IOException {

        logger.debug("Generating new Belief State from: {}", openApiYaml);

        Path tempPath;
        tempPath = Files.createTempFile("openapi-spec-entity", ".yaml");
        Files.writeString(tempPath, openApiYaml, StandardCharsets.UTF_8);

        var specFilePath = tempPath.toAbsolutePath().toString();
        var templatePath = Paths.get("src/main/resources/template/java/entity")
            .toAbsolutePath()
            .toString();

        var configurator = new CodegenConfigurator()
            .setInputSpec(specFilePath)
            .setGeneratorName("java-jpa-relation")
            .setOutputDir("generated")
            .setTemplateDir(templatePath)
            .addAdditionalProperty("gson", false)
            .addAdditionalProperty("jackson", false)
            .addAdditionalProperty("library", "native")
            .addAdditionalProperty("modelPropertyNaming", "original")
            .addAdditionalProperty("modelPackage", "org.ubiquia.acl.generated.entity")
            .addAdditionalProperty("useBeanValidation", true);

        configurator.setGlobalProperties(Map.of(
            "models", "",
            "modelDocs", "false",
            "modelTests", "false",
            "apis", "false",
            "apiDocs", "false",
            "apiTests", "false"
        ));

        var generator = new DefaultGenerator();
        generator.opts(configurator.toClientOptInput()).generate();
    }
}
