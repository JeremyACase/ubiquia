package org.ubiquia.core.belief.state.generator.service.decorator;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link InheritancePreprocessor}.
 *
 * <p>Includes a regression test for the pre-refactor bug where the preprocessor returned the
 * original schema string instead of the modified one.
 */
class InheritancePreprocessorTest {

    private InheritancePreprocessor preprocessor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        preprocessor = new InheritancePreprocessor();
        ReflectionTestUtils.setField(preprocessor, "objectMapper", objectMapper);
    }

    @Test
    void transform_plainObjectDefinition_addsAllOfWithAbstractDomainModelRef() throws Exception {
        var schema = """
            {
              "definitions": {
                "Person": {
                  "type": "object",
                  "properties": { "name": { "type": "string" } }
                }
              }
            }
            """;

        var result = preprocessor.transform(schema);

        var root = objectMapper.readTree(result);
        var personSchema = root.path("definitions").path("Person");

        assertThat(personSchema.has("allOf"))
            .as("Plain object definition should have allOf injected")
            .isTrue();
        assertThat(personSchema.path("allOf").get(0).path("$ref").asText())
            .isEqualTo("#/definitions/AbstractDomainModel");
    }

    @Test
    void transform_enumDefinition_isNotModified() throws Exception {
        var schema = """
            {
              "definitions": {
                "Color": {
                  "type": "string",
                  "enum": ["RED", "GREEN", "BLUE"]
                }
              }
            }
            """;

        var result = preprocessor.transform(schema);

        var root = objectMapper.readTree(result);
        var colorSchema = root.path("definitions").path("Color");

        assertThat(colorSchema.has("allOf"))
            .as("Enum definition should not have allOf injected")
            .isFalse();
    }

    @Test
    void transform_definitionWithExistingAllOf_isNotModified() throws Exception {
        var schema = """
            {
              "definitions": {
                "Animal": {
                  "allOf": [{"$ref": "#/definitions/AbstractDomainModel"}],
                  "properties": { "name": { "type": "string" } }
                }
              }
            }
            """;

        var result = preprocessor.transform(schema);

        var root = objectMapper.readTree(result);
        var animalAllOf = root.path("definitions").path("Animal").path("allOf");

        assertThat(animalAllOf.size())
            .as("Existing allOf should not be duplicated")
            .isEqualTo(1);
    }

    @Test
    void transform_returnsModifiedSchema_notOriginal() throws Exception {
        // Regression test for the pre-refactor bug: the method was returning the original
        // schema string (jsonSchema) instead of the modified string (modified).
        var schema = """
            {
              "definitions": {
                "Product": { "type": "object" }
              }
            }
            """;

        var result = preprocessor.transform(schema);

        // The result must contain the injected allOf — if it returned the original, this fails
        assertThat(result).contains("allOf");
        assertThat(result).contains("AbstractDomainModel");

        // The original schema string did not contain allOf
        assertThat(schema).doesNotContain("allOf");
    }

    @Test
    void transform_mixedDefinitions_onlyPlainObjectsGetAllOf() throws Exception {
        var schema = """
            {
              "definitions": {
                "Item": { "type": "object" },
                "Status": { "type": "string", "enum": ["ACTIVE", "INACTIVE"] },
                "WithParent": { "allOf": [{"$ref": "#/definitions/AbstractDomainModel"}] }
              }
            }
            """;

        var result = preprocessor.transform(schema);
        var root = objectMapper.readTree(result);

        assertThat(root.path("definitions").path("Item").has("allOf"))
            .as("Plain object should get allOf")
            .isTrue();
        assertThat(root.path("definitions").path("Status").has("allOf"))
            .as("Enum should not get allOf")
            .isFalse();
        assertThat(root.path("definitions").path("WithParent").path("allOf").size())
            .as("Existing allOf should not be duplicated")
            .isEqualTo(1);
    }

    @Test
    void transform_abstractDomainModel_isNotModified() throws Exception {
        var schema = """
            {
              "definitions": {
                "AbstractDomainModel": {
                  "type": "object",
                  "properties": { "id": { "type": "string" } }
                }
              }
            }
            """;

        var result = preprocessor.transform(schema);

        var root = objectMapper.readTree(result);
        var abstractDomainModel = root.path("definitions").path("AbstractDomainModel");

        assertThat(abstractDomainModel.has("allOf"))
            .as("AbstractDomainModel must not get a self-referencing allOf injected")
            .isFalse();
    }

    @Test
    void transform_keyValuePair_isNotModified() throws Exception {
        var schema = """
            {
              "definitions": {
                "KeyValuePair": {
                  "type": "object",
                  "properties": { "key": { "type": "string" }, "value": { "type": "string" } }
                }
              }
            }
            """;

        var result = preprocessor.transform(schema);

        var root = objectMapper.readTree(result);
        var keyValuePair = root.path("definitions").path("KeyValuePair");

        assertThat(keyValuePair.has("allOf"))
            .as("KeyValuePair must remain EMBEDDABLE — allOf must not be injected")
            .isFalse();
    }

    @Test
    void transform_doesNotMutateInputString() throws Exception {
        var original = """
            {
              "definitions": {
                "Order": { "type": "object" }
              }
            }
            """;

        var captured = original;
        preprocessor.transform(original);

        // Java strings are immutable, so this is always true — but we parse the input
        // and verify it was not changed as a parsed tree either
        assertThat(original).isEqualTo(captured);
        var root = objectMapper.readTree(original);
        assertThat(root.path("definitions").path("Order").has("allOf")).isFalse();
    }
}
