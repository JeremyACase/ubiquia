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
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.model.adapter.AdapterContext;
import org.ubiquia.core.flow.repository.AdapterRepository;

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
    private AdapterRepository adapterRepository;

    /**
     * Attempt to validate an input payload provided an adapter and its configured schema.
     *
     * @param inputPayload The input payload to validate.
     * @param adapter      The adapter we're validating an input payload for.
     * @throws ValidationException Exception from validating the payload.
     */
    public void tryValidateInputPayloadFor(
        final String inputPayload,
        final AbstractAdapter adapter)
        throws ValidationException {

        var adapterContext = adapter.getAdapterContext();
        if (adapterContext.getAdapterSettings().getValidateInputPayload()) {
            var schema = this.inputSchemaCache.get(adapterContext.getAdapterId());
            var validator = new net.jimblackler.jsonschemafriend.Validator();
            validator.validateJson(schema, inputPayload);
        }
    }

    /**
     * Attempt to validate an output payload provided an adapter and its configured schema.
     *
     * @param outputPayload The input payload to validate.
     * @param adapter       The adapter we're validating an input payload for.
     * @throws ValidationException Exception from validating the payload.
     */
    public void tryValidateOutputPayloadFor(
        final String outputPayload,
        final AbstractAdapter adapter)
        throws ValidationException {

        var adapterContext = adapter.getAdapterContext();

        if (adapterContext.getAdapterSettings().getValidateOutputPayload()) {
            var schema = this.outputSchemaCache.get(adapterContext.getAdapterId());
            var validator = new net.jimblackler.jsonschemafriend.Validator();
            validator.validateJson(schema, outputPayload);
        }
    }

    /**
     * Initialize our cache for an adapter if necessary.
     *
     * @param adapterContext The adapter to initialize.
     * @throws GenerationException Exception from generating our schema.
     */
    public void tryInitializeOutputSchema(final AdapterContext adapterContext)
        throws GenerationException {

        var adapterId = adapterContext.getAdapterId();
        if (!this.outputSchemaCache.containsKey(adapterId)) {
            logger.info("Initializing output schema cache for adapter {}...",
                adapterContext.getAdapterName());

            var record = this.adapterRepository.findById(adapterId);
            if (record.isEmpty()) {
                throw new RuntimeException("ERROR: Cannot find adapter: "
                    + adapterContext.getAdapterName());
            }

            var adapterEntity = record.get();

            if (Objects.nonNull(adapterEntity.getOutputSubSchema())) {
                var jsonSchema = adapterEntity
                    .getGraph()
                    .getAgentCommunicationLanguage()
                    .getJsonSchema();

                var schemaStore = new SchemaStore(true);
                var schema = schemaStore.loadSchemaJson(jsonSchema);

                var match = schema
                    .getSubSchemas()
                    .keySet()
                    .stream()
                    .filter(x -> x.toString().contains(adapterEntity.getOutputSubSchema().getModelName()))
                    .findFirst();

                if (match.isEmpty()) {
                    throw new RuntimeException("ERROR: Cannot find output subschema named  '"
                        + adapterEntity.getOutputSubSchema().getModelName()
                        + "' in ACL named '"
                        + adapterEntity.getGraph().getAgentCommunicationLanguage().getDomain()
                        + "'!");
                }

                var jsonSubSchema = schema.getSubSchemas().get(match.get());
                this.outputSchemaCache.put(adapterId, jsonSubSchema);
                logger.info("...initialized.");

            } else {
                logger.info("...no output schema defined; not initializing.");
            }
        }
    }

    /**
     * Initialize our cache for an adapter if necessary.
     *
     * @param adapterContext The adapter to initialize.
     * @throws GenerationException Exception from generating our schema.
     */
    public void tryInitializeInputPayloadSchema(final AdapterContext adapterContext)
        throws GenerationException, JsonProcessingException {

        var adapterId = adapterContext.getAdapterId();

        if (!this.inputSchemaCache.containsKey(adapterId)) {
            logger.info("Initializing input schema cache for adapter {}...",
                adapterContext.getAdapterName());

            var record = this.adapterRepository.findById(adapterId);
            if (record.isEmpty()) {
                throw new RuntimeException("ERROR: Cannot find adapter: "
                    + adapterContext.getAdapterName());
            }

            var entity = record.get();
            var jsonSchema = entity
                .getGraph()
                .getAgentCommunicationLanguage()
                .getJsonSchema();

            var schemaStore = new SchemaStore(true);
            var schema = schemaStore.loadSchemaJson(jsonSchema);

            URI jsonSubSchemaURI = null;

            for (var inputSchema : entity.getInputSubSchemas()) {

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

                var schemas = this.objectMapper.writeValueAsString(entity.getInputSubSchemas());

                throw new RuntimeException("ERROR: Cannot find match any input subschema  '"
                    + schemas
                    + "' in ACL named '"
                    + entity.getGraph().getAgentCommunicationLanguage().getDomain()
                    + "'!");
            }

            var jsonSubSchema = schema.getSubSchemas().get(jsonSubSchemaURI);
            this.inputSchemaCache.put(adapterId, jsonSubSchema);
            logger.info("...initialized.");
        }
    }
}
