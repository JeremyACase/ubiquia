package org.ubiquia.core.flow.service.visitor.validator;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import net.jimblackler.jsonschemafriend.SchemaStore;

/**
 * A service that can verify if incoming JSON Schemas are valid.
 */
@Service
public class JsonSchemaValidator {

    private static final Logger logger = LoggerFactory.getLogger(JsonSchemaValidator.class);

    /**
     * Validate whether or not a string representing a JSON Schema is in fact a valid
     * JSON Schema.
     *
     * @param jsonSchema The string representing a JSON Schema.
     * @return Whether or not the schema is valid.
     */
    public Boolean isValidJsonSchema(final String jsonSchema) {
        var valid = true;

        logger.debug("Validating JSON Schema: {}", jsonSchema);
        try {
            var schemaStore = new SchemaStore(false);
            schemaStore.loadSchemaJson(jsonSchema);

        } catch (Exception e) {
            logger.error("ERROR: JSON Schema was invalid: {}", e.getMessage());
            valid = false;
        }
        logger.debug("...JSON schema was valid.");
        return valid;
    }
}
