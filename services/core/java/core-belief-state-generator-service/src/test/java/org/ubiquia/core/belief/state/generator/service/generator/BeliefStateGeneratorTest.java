package org.ubiquia.core.belief.state.generator.service.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.ubiquia.common.model.ubiquia.dto.DomainDataContract;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;

@SpringBootTest
public class BeliefStateGeneratorTest {

    @Autowired
    private BeliefStateGenerator beliefStateGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${ubiquia.test.acl.schema.filepath}")
    private String schemaFilepath;

    @Test
    public void assertGeneratesBeliefState_isValid() throws IOException {

        var schemaPath = Paths.get(this.schemaFilepath);
        var jsonSchema = this.objectMapper.readValue(
            schemaPath.toFile(),
            Object.class);

        var acl = new DomainDataContract();
        acl.setJsonSchema(jsonSchema);
        acl.setName("pets");
        acl.setVersion(new SemanticVersion());
        acl.getVersion().setMajor(1);
        acl.getVersion().setMinor(2);
        acl.getVersion().setPatch(3);

        Assertions.assertDoesNotThrow(() ->
            this.beliefStateGenerator.generateBeliefStateFrom(acl));
    }
}
