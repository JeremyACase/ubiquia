package org.ubiquia.core.flow.service.visitor.validator;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.model.embeddable.SubSchema;

/**
 * A service dedicated to validating "Sub Schemas." In the future, this should be revamped
 * to do better subschema checks. I.e., it should be done in a JSON schema way.
 */
@Service
public class SubSchemaValidator {


    private static final Logger logger = LoggerFactory.getLogger(SubSchemaValidator.class);

    /**
     * Determine if two sub schemas are equivalent.
     *
     * @param left  The left sub schema.
     * @param right The right sub schema.
     * @return Whether or not the Sub Schemas are equivalent.
     */
    public Boolean areEquivalent(final SubSchema left, final SubSchema right) {

        var equivalent = true;

        if (!left.getModelName().equals(right.getModelName())) {
            logger.info("SubSchemas {} and {} do not refer to the same models!",
                left.getModelName(),
                right.getModelName());
            equivalent = false;
        }

        return equivalent;
    }
}
