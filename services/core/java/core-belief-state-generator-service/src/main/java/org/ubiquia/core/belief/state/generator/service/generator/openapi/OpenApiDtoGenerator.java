package org.ubiquia.core.belief.state.generator.service.generator.openapi;

import org.openapitools.codegen.config.CodegenConfigurator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Generates DTO source files from an OpenAPI YAML spec using the
 * {@code ubiquia-domain-dto-generator} codegen plugin.
 */
@Service
public class OpenApiDtoGenerator extends AbstractOpenApiGenerator {

    @Value("${ubiquia.beliefStateGeneratorService.template.filepath.dto}")
    private String templateFilepath;

    @Override
    protected String getGeneratorName() {
        return "ubiquia-domain-dto-generator";
    }

    @Override
    protected String getTemplatePath() {
        return this.templateFilepath;
    }

    @Override
    protected void configureAdditionalProperties(final CodegenConfigurator configurator) {
        configurator.addAdditionalProperty("javaxPackage", "jakarta");
    }
}
