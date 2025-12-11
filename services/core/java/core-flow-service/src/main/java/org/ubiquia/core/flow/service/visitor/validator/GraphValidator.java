package org.ubiquia.core.flow.service.visitor.validator;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Graph;

/**
 * A service dedicated to validating graphs are valid during registration.
 */
@Service
public class GraphValidator {

    private static final Logger logger = LoggerFactory.getLogger(GraphValidator.class);

    @Autowired
    private NodeValidator nodeValidator;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Attempt to validate whether a graph is valid.
     *
     * @param graph The registration object for the graph.
     * @throws Exception Exception thrown if the graph is not valid.
     */
    public void tryValidate(final Graph graph) throws Exception {
        logger.info("...validating graph with name {}... ", graph.getName());

        this.nodeValidator.tryValidate(graph.getNodes());
        this.tryValidateMatchingNodeSchemasFor(graph);
        logger.info("...graph named {} validated...", graph.getName());
    }

    /**
     * Attempt to validate if all of a graph's components have matching output/input
     * schemas.
     *
     * @param graph The graph to validate.
     */
    private void tryValidateMatchingNodeSchemasFor(final Graph graph)
        throws JsonProcessingException {

        logger.info("...validating matching input/output schemas...");
        for (var edge : graph.getEdges()) {

            logger.info("...validating edge hsa matching input/output: {}",
                this.objectMapper.writeValueAsString(edge));

            var outputNode = graph
                .getNodes()
                .stream()
                .filter(x -> x.getName().equals(edge.getLeftNodeName()))
                .findFirst()
                .get();

            for (var inputNodeName : edge.getRightNodeNames()) {

                var inputNode = graph
                    .getNodes()
                    .stream()
                    .filter(x -> x.getName().equals(inputNodeName))
                    .findFirst()
                    .get();

                var matchingInputOutput = this.nodeValidator.haveMatchingInputOutput(
                    outputNode,
                    inputNode);

                if (!matchingInputOutput) {
                    throw new IllegalArgumentException("ERROR: Mismatched schemas for edge with "
                        + outputNode
                        + " and "
                        + inputNode);
                }
            }
        }
    }
}
