package org.ubiquia.core.flow.service.builder;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.Random;
import net.jimblackler.jsongenerator.DefaultConfig;
import net.jimblackler.jsongenerator.Generator;
import net.jimblackler.jsongenerator.JsonGeneratorException;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.node.AbstractNode;
import org.ubiquia.core.flow.repository.NodeRepository;

@Service
public class StimulatedPayloadBuilder {

    private static final Logger logger = LoggerFactory.getLogger(StimulatedPayloadBuilder.class);
    @Autowired
    private NodeRepository nodeRepository;

    private HashMap<String, Schema> cachedNodeSchemas;

    private DefaultConfig jsonSchemaGeneratorConfiguration;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Constructor time.
     */
    public StimulatedPayloadBuilder() {
        this.cachedNodeSchemas = new HashMap<>();
        this.jsonSchemaGeneratorConfiguration = DefaultConfig.build()
            .setGenerateMinimal(false)
            .setNonRequiredPropertyChance(0.5f)
            .get();
    }

    public String buildStimulatedPayloadFor(final AbstractNode adapter)
        throws GenerationException, JsonProcessingException, JsonGeneratorException {

        logger.info("Building a dummy stimulation payload for adapter: {}...",
            adapter.getNodeContext().getNodeName());

        var adapterContext = adapter.getNodeContext();
        var jsonSchema = this.cachedNodeSchemas.get(adapterContext.getNodeId());
        var schemaStore = new SchemaStore(true);
        var generator = new Generator(
            this.jsonSchemaGeneratorConfiguration,
            schemaStore,
            new Random());
        var fuzzyData = generator.generate(jsonSchema, 10);
        var stringifiedPayload = this.objectMapper.writeValueAsString(fuzzyData);
        logger.debug("Generated dummy stimulation data: {}", stringifiedPayload);
        return stringifiedPayload;
    }

    /**
     * Attempt to initialize the JSON schema for the specific adapter.
     *
     * @param nodeId The id of the adapter to initialize a schema for.
     * @throws GenerationException Exception from generating dummy data.
     */
    @Transactional
    public void initializeSchema(final String nodeId) throws GenerationException {

        if (!this.cachedNodeSchemas.containsKey(nodeId)) {
            var record = this.nodeRepository.findById(nodeId);
            if (record.isEmpty()) {
                throw new RuntimeException("ERROR: Cannot find node with id: " + nodeId);
            }

            var nodeEntity = record.get();

            var jsonSchema = nodeEntity
                .getGraph()
                .getDomainOntology()
                .getDomainDataContract()
                .getSchema();

            var schemaStore = new SchemaStore(true);
            var schema = schemaStore.loadSchemaJson(jsonSchema);

            // Only assuming a single input for now; will need to update for MERGE adapters.
            var inputSchema = nodeEntity
                .getInputSubSchemas()
                .stream()
                .toList()
                .get(0);

            var match = schema
                .getSubSchemas()
                .keySet()
                .stream()
                .filter(x -> x.toString().contains(inputSchema.getModelName()))
                .findFirst();

            if (match.isEmpty()) {
                throw new RuntimeException("ERROR: Cannot find subschema named  '"
                    + inputSchema.getModelName()
                    + "' in domain ontology named '"
                    + nodeEntity.getGraph().getDomainOntology().getName()
                    + "'!");
            }
            var jsonSubSchema = schema.getSubSchemas().get(match.get());
            this.cachedNodeSchemas.put(nodeId, jsonSubSchema);
        }
    }
}
