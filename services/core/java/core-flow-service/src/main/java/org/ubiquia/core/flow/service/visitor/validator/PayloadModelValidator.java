package org.ubiquia.core.flow.service.visitor.validator;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.util.HashMap;
import java.util.Objects;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.node.AbstractNode;
import org.ubiquia.core.flow.model.node.NodeContext;
import org.ubiquia.core.flow.repository.NodeRepository;

/**
 * A service that can be used to validate whether incoming/outgoing payloads are valid.
 */
@Service
@Transactional
public class PayloadModelValidator {

    private static final Logger logger = LoggerFactory.getLogger(PayloadModelValidator.class);

    private final HashMap<String, Schema> inputSchemaCache = new HashMap<>();

    private final HashMap<String, Schema> outputSchemaCache = new HashMap<>();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NodeRepository nodeRepository;

    /**
     * Attempt to validate an input payload provided an adapter and its configured schema.
     *
     * @param inputPayload The input payload to validate.
     * @param node      The adapter we're validating an input payload for.
     * @throws ValidationException Exception from validating the payload.
     */
    public void tryValidateInputPayloadFor(
        final String inputPayload,
        final AbstractNode node)
        throws ValidationException {

        var nodeSettings = node.getNodeContext();
        if (nodeSettings.getNodeSettings().getValidateInputPayload()) {
            var schema = this.inputSchemaCache.get(nodeSettings.getNodeId());
            var validator = new net.jimblackler.jsonschemafriend.Validator();
            validator.validateJson(schema, inputPayload);
        }
    }

    /**
     * Attempt to validate an output payload provided an adapter and its configured schema.
     *
     * @param outputPayload The input payload to validate.
     * @param node       The adapter we're validating an input payload for.
     * @throws ValidationException Exception from validating the payload.
     */
    public void tryValidateOutputPayloadFor(
        final String outputPayload,
        final AbstractNode node)
        throws ValidationException {

        var nodeContext = node.getNodeContext();

        if (nodeContext.getNodeSettings().getValidateOutputPayload()) {
            var schema = this.outputSchemaCache.get(nodeContext.getNodeId());
            var validator = new net.jimblackler.jsonschemafriend.Validator();
            validator.validateJson(schema, outputPayload);
        }
    }

    /**
     * Initialize our cache for an adapter if necessary.
     *
     * @param nodeContext The adapter to initialize.
     * @throws GenerationException Exception from generating our schema.
     */
    public void tryInitializeOutputSchema(final NodeContext nodeContext)
        throws GenerationException {

        var nodeId = nodeContext.getNodeId();
        if (!this.outputSchemaCache.containsKey(nodeId)) {
            logger.info("Initializing output schema cache for node {}...",
                nodeContext.getNodeName());

            var record = this.nodeRepository.findById(nodeId);
            if (record.isEmpty()) {
                throw new RuntimeException("ERROR: Cannot find node: "
                    + nodeContext.getNodeName());
            }

            var nodeEntity = record.get();

            if (Objects.nonNull(nodeEntity.getOutputSubSchema())) {

                var jsonSchema = nodeEntity
                    .getGraph()
                    .getDomainOntology()
                    .getDomainDataContract()
                    .getJsonSchema();

                var schemaStore = new SchemaStore(true);
                var schema = schemaStore.loadSchemaJson(jsonSchema);

                var match = schema
                    .getSubSchemas()
                    .keySet()
                    .stream()
                    .filter(x -> x.toString().contains(nodeEntity.getOutputSubSchema().getModelName()))
                    .findFirst();

                if (match.isEmpty()) {
                    throw new RuntimeException("ERROR: Cannot find output subschema named  '"
                        + nodeEntity.getOutputSubSchema().getModelName()
                        + "' in data contract named '"
                        + nodeEntity.getGraph().getDomainOntology().getName()
                        + "'!");
                }

                var jsonSubSchema = schema.getSubSchemas().get(match.get());
                this.outputSchemaCache.put(nodeId, jsonSubSchema);
                logger.info("...initialized.");

            } else {
                logger.info("...no output schema defined; not initializing.");
            }
        }
    }

    /**
     * Initialize our cache for an adapter if necessary.
     *
     * @param nodeContext The adapter to initialize.
     * @throws GenerationException Exception from generating our schema.
     */
    public void tryInitializeInputPayloadSchema(final NodeContext nodeContext)
        throws GenerationException, JsonProcessingException {

        var nodeId = nodeContext.getNodeId();

        if (!this.inputSchemaCache.containsKey(nodeId)) {
            logger.info("Initializing input schema cache for adapter {}...",
                nodeContext.getNodeName());

            var record = this.nodeRepository.findById(nodeId);
            if (record.isEmpty()) {
                throw new RuntimeException("ERROR: Cannot find adapter: "
                    + nodeContext.getNodeName());
            }

            var nodeEntity = record.get();
            var jsonSchema = nodeEntity
                .getGraph()
                .getDomainOntology()
                .getDomainDataContract()
                .getJsonSchema();

            var schemaStore = new SchemaStore(true);
            var schema = schemaStore.loadSchemaJson(jsonSchema);

            URI jsonSubSchemaURI = null;

            for (var inputSchema : nodeEntity.getInputSubSchemas()) {

                var match = schema
                    .getSubSchemas()
                    .keySet()
                    .stream()
                    .filter(x -> x.toString().contains(inputSchema.getModelName()))
                    .findFirst();

                if (match.isPresent()) {
                    jsonSubSchemaURI = match.get();
                    break;
                }
            }

            if (Objects.isNull(jsonSubSchemaURI)) {

                var schemas = this.objectMapper.writeValueAsString(nodeEntity.getInputSubSchemas());

                throw new RuntimeException("ERROR: Cannot find match any input subschema  '"
                    + schemas
                    + "' in ACL named '"
                    + nodeEntity.getGraph().getDomainOntology().getName()
                    + "'!");
            }

            var jsonSubSchema = schema.getSubSchemas().get(jsonSubSchemaURI);
            this.inputSchemaCache.put(nodeId, jsonSubSchema);
            logger.info("...initialized.");
        }
    }
}
