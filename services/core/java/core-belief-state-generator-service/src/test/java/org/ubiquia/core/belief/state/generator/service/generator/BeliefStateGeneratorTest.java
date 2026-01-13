package org.ubiquia.core.belief.state.generator.service.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.ubiquia.common.model.ubiquia.dto.DomainDataContract;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;

@SpringBootTest
public class BeliefStateGeneratorTest {

    @Autowired
    private BeliefStateGenerator beliefStateGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${ubiquia.test.ontology.filepath}")
    private String ontologyFilepath;

    @Test
    public void assertGeneratesBeliefState_isValid() throws IOException {

        var ontologyFilepath = Paths.get(this.ontologyFilepath);
        var yamlMapper = new ObjectMapper(new YAMLFactory());
        var domainOntology = yamlMapper
            .readValue(ontologyFilepath.toFile(), DomainOntology.class);

        Assertions.assertDoesNotThrow(() -> this
            .beliefStateGenerator
            .generateBeliefStateFrom(domainOntology));
    }
}
