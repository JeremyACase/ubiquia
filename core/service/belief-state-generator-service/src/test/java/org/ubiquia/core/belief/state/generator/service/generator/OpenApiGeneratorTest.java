package org.ubiquia.core.belief.state.generator.service.generator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.ubiquia.core.belief.state.generator.service.generator.openapi.OpenApiEntityGenerator;

@SpringBootTest
public class OpenApiGeneratorTest {

    @Autowired
    private OpenApiEntityGenerator openApiEntityGenerator;

    @Value("${ubiquia.test.acl.openapi.filepath}")
    private String openapiFilepath;

    @Test
    public void assertGeneratesOpenApi_isValid() throws IOException {

        var openapiPath = Paths.get(this.openapiFilepath);
        var yamlString = Files.readString(openapiPath, StandardCharsets.UTF_8);

        Assertions.assertDoesNotThrow(() ->
            this.openApiEntityGenerator.generateOpenApiEntitiesFrom(yamlString));
    }
}
