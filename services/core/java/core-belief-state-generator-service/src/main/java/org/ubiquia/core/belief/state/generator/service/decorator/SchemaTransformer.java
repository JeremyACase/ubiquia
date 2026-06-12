package org.ubiquia.core.belief.state.generator.service.decorator;

import java.io.IOException;

/**
 * Transforms a JSON schema string and returns the result.
 *
 * <p>Implementations are applied in sequence by {@link SchemaTransformationPipeline}.
 * Use {@code @Order} on each implementation to control execution order.
 */
@FunctionalInterface
public interface SchemaTransformer {

    /**
     * Applies this transformation to the given JSON schema string.
     *
     * @param schema the input JSON schema
     * @return the transformed JSON schema
     * @throws IOException if JSON parsing or serialization fails
     */
    String transform(String schema) throws IOException;
}
