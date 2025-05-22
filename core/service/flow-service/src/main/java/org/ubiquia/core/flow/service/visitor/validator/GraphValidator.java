package org.ubiquia.core.flow.service.visitor.validator;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.models.dto.GraphDto;
import org.ubiquia.common.models.entity.Graph;
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

    /**
     * Attempt to validate whether a graph is valid.
     *
     * @param graphEntity       The database data for the graph.
     * @param graphRegistration The registration object for the graph.
     * @throws Exception Exception thrown if the graph is not valid.
     */
    public void tryValidate(
        final Graph graphEntity,
        final GraphDto graphRegistration)
        throws Exception {
        logger.info("...validating graph with name {} and version {}... ",
            graphEntity.getGraphName(),
            graphEntity.getVersion());

        this.adapterValidator.tryValidate(graphEntity.getAdapters());
        this.tryValidateMatchingAdapterSchemasFor(graphRegistration);
        logger.info("...{} validated...", graphEntity.getGraphName());
    }

    /**
     * Attempt to validate if all of a graph's agents have matching output/input
     * schemas.
     *
     * @param graphRegistration The graph to validate.
     */
    private void tryValidateMatchingAdapterSchemasFor(
        final GraphDto graphRegistration) {

        logger.info("...validating matching input/output schemas...");
        for (var edge : graphRegistration.getEdges()) {

            logger.info("...validating edge: {}", edge);

            var leftAdapterRecord = this.adapterRepository
                .findByGraphGraphNameAndAdapterName(
                    graphRegistration.getGraphName(),
                    edge.getLeftAdapterName());

            if (leftAdapterRecord.isEmpty()) {
                throw new IllegalArgumentException("ERROR: Could not find adapter "
                    + edge.getLeftAdapterName()
                    + " for graph "
                    + graphRegistration.getGraphName());
            }

            for (var right : edge.getRightAdapterNames()) {
                var rightAdapterRecord = this.adapterRepository
                    .findByGraphGraphNameAndAdapterName(
                        graphRegistration.getGraphName(),
                        right);

                if (rightAdapterRecord.isEmpty()) {
                    throw new IllegalArgumentException("ERROR: Could not find adapter "
                        + right
                        + " for graph "
                        + graphRegistration.getGraphName());
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
