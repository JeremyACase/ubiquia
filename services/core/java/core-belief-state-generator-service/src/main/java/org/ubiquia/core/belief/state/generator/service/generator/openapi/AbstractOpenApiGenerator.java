package org.ubiquia.core.belief.state.generator.service.generator.openapi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Template for OpenAPI code generators that write a YAML spec to a temp file
 * and drive a {@link DefaultGenerator} to produce Java source files.
 *
 * <p>Subclasses supply the generator name and template path; they may also override
 * {@link #configureAdditionalProperties} and {@link #buildGlobalProperties} to inject
 * generator-specific settings without duplicating the common setup.
 */
public abstract class AbstractOpenApiGenerator {

    private static final Logger logger =
        LoggerFactory.getLogger(AbstractOpenApiGenerator.class);

    /**
     * Returns the OpenAPI generator name (e.g. {@code "ubiquia-domain-dto-generator"}).
     *
     * @return the codegen generator name
     */
    protected abstract String getGeneratorName();

    /**
     * Returns the absolute or Spring-resolved path to the Mustache template directory.
     *
     * @return the template directory path
     */
    protected abstract String getTemplatePath();

    /**
     * Hook for subclasses to add extra additional properties to the configurator.
     *
     * @param configurator the configurator being built
     */
    protected void configureAdditionalProperties(final CodegenConfigurator configurator) {
        // default: no additional properties
    }

    /**
     * Builds the global properties map passed to the configurator.
     *
     * <p>Subclasses may call {@code super.buildGlobalProperties()}, add entries, and return
     * the extended map.
     *
     * @return mutable map of global codegen properties
     */
    protected Map<String, String> buildGlobalProperties() {
        var props = new HashMap<String, String>();
        props.put("models", "");
        props.put("modelDocs", "false");
        props.put("modelTests", "false");
        props.put("apis", "false");
        props.put("apiDocs", "false");
        props.put("apiTests", "false");
        return props;
    }

    /**
     * Writes {@code openApiYaml} to a temp file and runs the generator.
     *
     * @param openApiYaml the OpenAPI YAML spec string
     * @throws IOException if the temp file cannot be written
     */
    public final void generate(final String openApiYaml) throws IOException {

        logger.debug("Running {} on spec: {}",
            this.getClass().getSimpleName(), openApiYaml);

        var tempPath = Files.createTempFile("openapi-spec", ".yaml");
        Files.writeString(tempPath, openApiYaml, StandardCharsets.UTF_8);

        var specFilePath = tempPath.toAbsolutePath().toString();
        var templatePath = Paths.get(this.getTemplatePath())
            .toAbsolutePath()
            .toString();

        var configurator = new CodegenConfigurator()
            .setInputSpec(specFilePath)
            .setGeneratorName(this.getGeneratorName())
            .setOutputDir("generated")
            .setTemplateDir(templatePath)
            .addAdditionalProperty("gson", false)
            .addAdditionalProperty("jackson", false)
            .addAdditionalProperty("library", "native")
            .addAdditionalProperty("modelPropertyNaming", "original")
            .addAdditionalProperty("modelPackage", "org.ubiquia.domain.generated")
            .addAdditionalProperty("useBeanValidation", true)
            .addAdditionalProperty("openApiNullable", false);

        this.configureAdditionalProperties(configurator);
        configurator.setGlobalProperties(this.buildGlobalProperties());

        new DefaultGenerator().opts(configurator.toClientOptInput()).generate();
    }
}
