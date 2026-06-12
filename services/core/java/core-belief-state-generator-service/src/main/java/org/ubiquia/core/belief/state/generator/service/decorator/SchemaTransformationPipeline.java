package org.ubiquia.core.belief.state.generator.service.decorator;

import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Applies a sequence of {@link SchemaTransformer}s to a JSON schema string.
 *
 * <p>Spring injects all {@code SchemaTransformer} beans in {@code @Order}-defined sequence.
 * Each transformer receives the output of the previous one, forming a pipeline.
 */
@Service
public class SchemaTransformationPipeline {

    private static final Logger logger =
        LoggerFactory.getLogger(SchemaTransformationPipeline.class);

    @Autowired
    private List<SchemaTransformer> transformers;

    /**
     * Runs the schema through each registered transformer in order.
     *
     * @param schema the initial JSON schema string
     * @return the fully transformed JSON schema string
     * @throws IOException if any transformer fails to parse or serialize JSON
     */
    public String apply(final String schema) throws IOException {
        var result = schema;
        for (var transformer : this.transformers) {
            logger.debug("Applying transformer: {}", transformer.getClass().getSimpleName());
            result = transformer.transform(result);
        }
        return result;
    }
}
