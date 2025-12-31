package org.ubiquia.core.belief.state.generator.service.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.core.belief.state.generator.service.compile.BeliefStateCompiler;
import org.ubiquia.core.belief.state.generator.service.compile.BeliefStateUberizer;
import org.ubiquia.core.belief.state.generator.service.decorator.EnumNormalizer;
import org.ubiquia.core.belief.state.generator.service.decorator.InheritancePreprocessor;
import org.ubiquia.core.belief.state.generator.service.decorator.UbiquiaModelInjector;
import org.ubiquia.core.belief.state.generator.service.generator.openapi.OpenApiDtoGenerator;
import org.ubiquia.core.belief.state.generator.service.generator.openapi.OpenApiEntityGenerator;
import org.ubiquia.core.belief.state.generator.service.generator.postprocess.BeliefStateGenerationPostProcessor;
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
    private BeliefStateGenerationPostProcessor beliefStateGenerationPostProcessor;

    @Autowired
    private EnumNormalizer enumNormalizer;

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

    /**
     * Given a domain ontology, attempt to generate and deploy a new Belief State for it.
     *
     * @param domainOntology The Domain Ontology we're generating a belief state for.
     * @throws Exception Exceptions from a lot of stuff.
     */
    public void generateBeliefStateFrom(final DomainOntology domainOntology)
        throws Exception {

        logger.info("Generating new Belief State from: {}", this
            .objectMapper
            .writeValueAsString(domainOntology));

        var jsonSchema = this.getJsonSchemaFrom(domainOntology);

        var openApiEntityYaml = this
            .jsonSchemaToOpenApiEntityYamlMapper
            .translateJsonSchemaToOpenApiYaml(jsonSchema);

        this.openApiEntityGenerator.generateOpenApiEntitiesFrom(openApiEntityYaml);

        var openApiDtoYaml = this
            .jsonSchemaToOpenApiDtoYamlMapper
            .translateJsonSchemaToOpenApiYaml(jsonSchema);
        this.openApiDtoGenerator.generateOpenApiDtosFrom(openApiDtoYaml);

        this.beliefStateGenerationPostProcessor.postProcess(domainOntology);

        var beliefStateLibraries = this.getLibraryJarPaths(this.beliefStateLibsDirectory);

        this
            .beliefStateCompiler
            .compileGeneratedSources("generated", "compiled", beliefStateLibraries);

        var beliefStateName = domainOntology
            .getName().toLowerCase()
            + "-"
            + domainOntology.getVersion().toString()
            + ".jar";

        var jarPath = this.uberJarsPath + beliefStateName;
        this
            .beliefStateUberizer
            .createUberJar(jarPath, "compiled", beliefStateLibraries);

        if (Objects.nonNull(this.beliefStateOperator)) {
            this.beliefStateOperator.tryDeployBeliefState(domainOntology);
        }
    }

    /**
     * Given a domain ontology, build a JSON Schema to use to generate a new belief state.
     *
     * @param domainOntology The domain ontology we're generating a belief state for.
     * @return The stringified JSON schema.
     * @throws IOException Exception from IO.
     */
    private String getJsonSchemaFrom(final DomainOntology domainOntology)
        throws IOException {

        var jsonSchema = this
            .objectMapper
            .writeValueAsString(domainOntology.getDomainDataContract().getJsonSchema());
        jsonSchema = this.enumNormalizer.normalizeEnums(jsonSchema);
        jsonSchema = this.ubiquiaModelInjector.appendAclModels(jsonSchema);
        jsonSchema = this.inheritancePreprocessor.appendInheritance(jsonSchema);

        logger.debug("Preprocessed JSON Schema to: {}", jsonSchema);

        return jsonSchema;
    }

    /**
     * Given a location for Belief State libraries, build and return the appropriate
     * .jar path dependencies.
     *
     * @param libsDirPath The path where the libraries exist at.
     * @return A list of fully-qualified .jar file paths.
     */
    private List<String> getLibraryJarPaths(final String libsDirPath) {
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
