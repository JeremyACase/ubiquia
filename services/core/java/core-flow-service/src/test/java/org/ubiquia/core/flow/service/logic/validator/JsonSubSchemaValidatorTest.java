package org.ubiquia.core.flow.service.logic.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.ubiquia.core.flow.service.visitor.validator.JsonSchemaValidator;


@SpringBootTest
@AutoConfigureMockMvc
public class JsonSubSchemaValidatorTest {

    @Value("${ubiquia.test.acl.schema.filepath}")
    private String filepath;

    @Autowired
    private JsonSchemaValidator jsonSchemaValidator;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void assertJsonSchema_isValid() throws Exception {
        var path = Paths.get(this.filepath);

        var jsonSchema = this.objectMapper.readValue(
            path.toFile(),
            Object.class);

        var valid = this.jsonSchemaValidator.isValidJsonSchema(
            this.objectMapper.writeValueAsString(jsonSchema));
        Assertions.assertTrue(valid);
    }

    @Test
    public void assertJsonSchema_isInvalid() throws Exception {

        var valid = this.jsonSchemaValidator.isValidJsonSchema(
            this.objectMapper.writeValueAsString("invalid"));
        Assertions.assertFalse(valid);
    }
}