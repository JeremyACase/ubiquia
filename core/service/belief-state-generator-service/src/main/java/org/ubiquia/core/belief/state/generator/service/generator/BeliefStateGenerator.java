package org.ubiquia.core.belief.state.generator.service.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguage;
import org.ubiquia.core.belief.state.generator.service.compile.BeliefStateCompiler;
import org.ubiquia.core.belief.state.generator.service.compile.BeliefStateUberizer;
import org.ubiquia.core.belief.state.generator.service.decorator.EnumNormalizer;
import org.ubiquia.core.belief.state.generator.service.decorator.InheritancePreprocessor;
import org.ubiquia.core.belief.state.generator.service.decorator.UbiquiaModelInjector;
import org.ubiquia.core.belief.state.generator.service.generator.openapi.OpenApiDtoGenerator;
import org.ubiquia.core.belief.state.generator.service.generator.openapi.OpenApiEntityGenerator;
import org.ubiquia.core.belief.state.generator.service.k8s.BeliefStateOperator;
import org.ubiquia.core.belief.state.generator.service.mapper.JsonSchemaToOpenApiDtoYamlMapper;
import org.ubiquia.core.belief.state.generator.service.mapper.JsonSchemaToOpenApiEntityYamlMapper;

@Service
public class BeliefStateGenerator {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateGenerator.class);

    @Value("${ubiquia.beliefStateGeneratorService.libraries.directory.path}")
    private String beliefStateLibsDirectory;

    @Value("${ubiquia.beliefStateGeneratorService.uber.jars.path}")
    private String uberJarsPath;

    @Autowired
    private BeliefStateCompiler beliefStateCompiler;

    @Autowired(required = false)
    private BeliefStateOperator beliefStateOperator;

    @Autowired
    private EnumNormalizer enumNormalizer;

    @Autowired
    private GenerationCleanupProcessor generationCleanupProcessor;

    @Autowired
    private GenerationSupportProcessor generationSupportProcessor;

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

    @Autowired
    private BeliefStateUberizer beliefStateUberizer;

    public void generateBeliefStateFrom(final AgentCommunicationLanguage acl)
        throws Exception {

        logger.info("Generating new Belief State from: {}",
            this.objectMapper.writeValueAsString(acl));

        var jsonSchema = this.getJsonSchemaFrom(acl);

        var openApiEntityYaml =
            this.jsonSchemaToOpenApiEntityYamlMapper
                .translateJsonSchemaToOpenApiYaml(jsonSchema);
        this.openApiEntityGenerator.generateOpenApiEntitiesFrom(openApiEntityYaml);

        var openApiDtoYaml =
            this.jsonSchemaToOpenApiDtoYamlMapper
                .translateJsonSchemaToOpenApiYaml(jsonSchema);
        this.openApiDtoGenerator.generateOpenApiDtosFrom(openApiDtoYaml);

        this.generationCleanupProcessor.removeBlacklistedFiles(Paths.get("generated"));
        this.generationSupportProcessor.postProcess(acl);

        var beliefStateLibraries = this.getJarPaths(this.beliefStateLibsDirectory);

        this.beliefStateCompiler.compileGeneratedSources(
            "generated",
            "compiled",
            beliefStateLibraries);

        var beliefStateName =
            acl.getDomain().toLowerCase()
                + "-"
                + acl.getVersion().toString()
                + ".jar";

        var jarPath = this.uberJarsPath + beliefStateName;
        this.beliefStateUberizer.createUberJar(
            jarPath,
            "compiled",
            beliefStateLibraries
        );

        if (Objects.nonNull(this.beliefStateOperator)) {
            this.beliefStateOperator.tryDeployBeliefState(acl);
        }
    }

    private String getJsonSchemaFrom(final AgentCommunicationLanguage acl)
        throws IOException {

        var jsonSchema = this.objectMapper.writeValueAsString(acl.getJsonSchema());
        jsonSchema = this.enumNormalizer.normalizeEnums(jsonSchema);
        jsonSchema = this.ubiquiaModelInjector.appendAclModels(jsonSchema);
        jsonSchema = this.inheritancePreprocessor.appendInheritance(jsonSchema);

        logger.debug("Preprocessed JSON Schema to: {}", jsonSchema);

        return jsonSchema;
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
