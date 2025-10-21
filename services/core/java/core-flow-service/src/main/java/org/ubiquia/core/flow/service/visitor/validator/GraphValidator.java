package org.ubiquia.core.flow.service.visitor.validator;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;
import org.ubiquia.core.flow.repository.AdapterRepository;

/**
 * A service dedicated to validating graphs are valid during registration.
 */
@Service
public class GraphValidator {

    private static final Logger logger = LoggerFactory.getLogger(GraphValidator.class);

    @Autowired
    private AdapterValidator adapterValidator;

    @Autowired
    private AdapterRepository adapterRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Attempt to validate whether a graph is valid.
     *
     * @param graphEntity       The database data for the graph.
     * @param graphRegistration The registration object for the graph.
     * @throws Exception Exception thrown if the graph is not valid.
     */
    public void tryValidate(
        final GraphEntity graphEntity,
        final Graph graphRegistration)
        throws Exception {
        logger.info("...validating graph with name {} and version {}... ",
            graphEntity.getName(),
            graphEntity.getVersion());

        this.adapterValidator.tryValidate(graphEntity.getAdapters());
        this.tryValidateMatchingAdapterSchemasFor(graphRegistration);
        logger.info("...{} validated...", graphEntity.getName());
    }

    /**
     * Attempt to validate if all of a graph's components have matching output/input
     * schemas.
     *
     * @param graphRegistration The graph to validate.
     */
    private void tryValidateMatchingAdapterSchemasFor(
        final Graph graphRegistration) throws JsonProcessingException {

        logger.info("...validating matching input/output schemas...");
        for (var edge : graphRegistration.getEdges()) {

            logger.info("...validating edge hsa matching input/output: {}",
                this.objectMapper.writeValueAsString(edge));

            var leftAdapterRecord = this.adapterRepository
                .findByGraphNameAndName(
                    graphRegistration.getName(),
                    edge.getLeftAdapterName());

            if (leftAdapterRecord.isEmpty()) {
                throw new IllegalArgumentException("ERROR: Could not find adapter "
                    + edge.getLeftAdapterName()
                    + " for graph "
                    + graphRegistration.getName());
            }

            for (var right : edge.getRightAdapterNames()) {
                var rightAdapterRecord = this.adapterRepository
                    .findByGraphNameAndName(
                        graphRegistration.getName(),
                        right);

                if (rightAdapterRecord.isEmpty()) {
                    throw new IllegalArgumentException("ERROR: Could not find adapter "
                        + right
                        + " for graph "
                        + graphRegistration.getName());
                }

                var matchingInputOutput = this.adapterValidator
                    .haveMatchingInputOutput(leftAdapterRecord.get(), rightAdapterRecord.get());

                if (!matchingInputOutput) {
                    throw new IllegalArgumentException("ERROR: Mismatched schemas for edge with "
                        + edge.getLeftAdapterName()
                        + " and "
                        + right);
                }
            }
        }
    }
}
