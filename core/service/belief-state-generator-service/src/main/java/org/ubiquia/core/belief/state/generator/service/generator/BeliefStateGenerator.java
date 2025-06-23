package org.ubiquia.core.belief.state.generator.service.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguageDto;
import org.ubiquia.core.belief.state.generator.service.compiler.BeliefStateCompiler;
import org.ubiquia.core.belief.state.generator.service.decorator.InheritancePreprocessor;
import org.ubiquia.core.belief.state.generator.service.decorator.UbiquiaModelInjector;
import org.ubiquia.core.belief.state.generator.service.generator.openapi.OpenApiDtoGenerator;
import org.ubiquia.core.belief.state.generator.service.generator.openapi.OpenApiEntityGenerator;
import org.ubiquia.core.belief.state.generator.service.mapper.JsonSchemaToOpenApiDtoYamlMapper;
import org.ubiquia.core.belief.state.generator.service.mapper.JsonSchemaToOpenApiEntityYamlMapper;

@Service
public class BeliefStateGenerator {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateGenerator.class);

    @Autowired
    private BeliefStateCompiler beliefStateCompiler;

    @Autowired
    private GenerationCleanupProcessor generationCleanupProcessor;

    @Autowired
    private InheritancePreprocessor inheritancePreprocessor;

    @Autowired
    private JsonSchemaToOpenApiDtoYamlMapper jsonSchemaToOpenApiDtoYamlMapper;

    @Autowired
    private JsonSchemaToOpenApiEntityYamlMapper jsonSchemaToOpenApiEntityYamlMapper;

    @Autowired
    private OpenApiEntityGenerator openApiEntityGenerator;

    @Autowired
    private OpenApiDtoGenerator openApiDtoGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UbiquiaModelInjector ubiquiaModelInjector;

    public void generateBeliefStateFrom(final AgentCommunicationLanguageDto acl)
        throws Exception {

        logger.info("Generating new Belief State from: {}",
            this.objectMapper.writeValueAsString(acl));

        var jsonSchema = this.objectMapper.writeValueAsString(acl.getJsonSchema());
        jsonSchema = this.ubiquiaModelInjector.appendAclModels(jsonSchema);
        jsonSchema = this.inheritancePreprocessor.appendInheritance(jsonSchema);

        var openApiEntityYaml =
            this.jsonSchemaToOpenApiEntityYamlMapper
                .translateJsonSchemaToOpenApiYaml(jsonSchema);
        this.openApiEntityGenerator.generateOpenApiEntitiesFrom(openApiEntityYaml);

        var openApiDtoYaml =
            this.jsonSchemaToOpenApiDtoYamlMapper
                .translateJsonSchemaToOpenApiYaml(jsonSchema);
        this.openApiDtoGenerator.generateOpenApiDtosFrom(openApiDtoYaml);

        this.generationCleanupProcessor.removeBlacklistedFiles(Paths.get("generated"));

        var beliefStateLibraries = this.getJarPaths("belief-state-libs");

        this.beliefStateCompiler.compileGeneratedSources(
            "generated",
            "compiled",
            beliefStateLibraries);
    }

    private List<String> getJarPaths(final String libsDirPath) {
        var libsDir = new File(libsDirPath);
        var jarPaths = new ArrayList<String>();

        if (libsDir.exists() && libsDir.isDirectory()) {
            for (var file : libsDir.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".jar")) {
                    logger.debug("...found belief state library dependency: {}",
                        file.getAbsolutePath());
                    jarPaths.add(file.getAbsolutePath());
                }
            }
        }

        return jarPaths;
    }
}
