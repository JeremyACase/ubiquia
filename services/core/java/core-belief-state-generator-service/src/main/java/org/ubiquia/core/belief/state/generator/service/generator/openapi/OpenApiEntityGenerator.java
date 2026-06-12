package org.ubiquia.core.belief.state.generator.service.generator.openapi;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Generates JPA entity source files from an OpenAPI YAML spec using the
 * {@code ubiquia-domain-entity-generator} codegen plugin.
 */
@Service
public class OpenApiEntityGenerator extends AbstractOpenApiGenerator {

    @Value("${ubiquia.beliefStateGeneratorService.template.filepath.entity}")
    private String templateFilepath;

    @Override
    protected String getGeneratorName() {
        return "ubiquia-domain-entity-generator";
    }

    @Override
    protected String getTemplatePath() {
        return this.templateFilepath;
    }

    @Override
    protected Map<String, String> buildGlobalProperties() {
        var props = super.buildGlobalProperties();
        props.put("supportingFiles", "false");
        return props;
    }
}
