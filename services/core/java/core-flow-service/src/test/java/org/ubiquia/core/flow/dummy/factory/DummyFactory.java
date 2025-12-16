package org.ubiquia.core.flow.dummy.factory;

import static org.instancio.Select.field;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.instancio.Instancio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.*;
import org.ubiquia.common.model.ubiquia.embeddable.NodeSettings;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;
import org.ubiquia.common.model.ubiquia.embeddable.SubSchema;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;


@Service
public class DummyFactory {

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${ubiquia.test.acl.schema.filepath}")
    private String schemaFilepath;

    /**
     * Helper method to build a subschema provided a name.
     *
     * @param fromModelName The name to build a subschema for.
     * @return A subschema.
     */
    public SubSchema buildSubSchema(final String fromModelName) {
        var subSchema = new SubSchema();
        subSchema.setModelName(fromModelName);
        return subSchema;
    }

    /**
     * Generate an Adapter for testing.
     *
     * @return A dummy adapter.
     */
    public Node generateNode() {
        var adapter = Instancio
            .of(Node.class)
            .ignore(field(AbstractModel::getId))
            .ignore(field(AbstractModel::getCreatedAt))
            .ignore(field(AbstractModel::getUpdatedAt))
            .ignore(field(Node::getGraph))
            .ignore(field(Node::getBrokerSettings))
            .ignore(field(Node::getComponent))
            .ignore(field(Node::getDownstreamNodes))
            .ignore(field(Node::getEgressSettings))
            .ignore(field(Node::getOutputSubSchema))
            .ignore(field(Node::getPollSettings))
            .ignore(field(Node::getOverrideSettings))
            .ignore(field(Node::getNodeType))
            .set(field(Node::getNodeSettings), new NodeSettings())
            .set(field(Node::getFlowEvents), new ArrayList<>())
            .set(field(Node::getInputSubSchemas), new ArrayList<>())
            .set(field(AbstractModel::getModelType), "Adapter")
            .create();
        return adapter;
    }

    public DomainOntology generateDomainOntology() throws IOException {

        var graphs = new ArrayList<Graph>();
        graphs.add(this.generateGraph());

        var domainOntology = Instancio
            .of(DomainOntology.class)
            .ignore(field(AbstractModel::getId))
            .ignore(field(AbstractModel::getCreatedAt))
            .ignore(field(AbstractModel::getUpdatedAt))
            .set(field(AbstractModel::getModelType), "DomainOntology")
            .set(field(DomainOntology::getVersion), this.getSemanticVersion())
            .set(field(DomainOntology::getDomainDataContract), this.generateDomainContract())
            .set(field(DomainOntology::getGraphs), graphs)
            .create();

        return domainOntology;

    }

    public DomainDataContract generateDomainContract() throws IOException {

        var schemaPath = Paths.get(this.schemaFilepath);
        var jsonSchema = this.objectMapper.readValue(
            schemaPath.toFile(),
            Object.class);

        var domainDataContract = Instancio
            .of(DomainDataContract.class)
            .ignore(field(AbstractModel::getId))
            .ignore(field(AbstractModel::getCreatedAt))
            .ignore(field(AbstractModel::getUpdatedAt))
            .set(field(AbstractModel::getModelType), "Component")
            .set(field(DomainDataContract::getJsonSchema), jsonSchema)
            .create();
        return domainDataContract;
    }

    /**
     * Generate a component for testing.
     *
     * @return A component.
     */
    public Component generateComponent() {
        var component = Instancio
            .of(Component.class)
            .ignore(field(AbstractModel::getId))
            .ignore(field(AbstractModel::getCreatedAt))
            .ignore(field(AbstractModel::getUpdatedAt))
            .ignore(field(Component::getNode))
            .ignore(field(Component::getConfig))
            .ignore(field(Component::getEnvironmentVariables))
            .ignore(field(Component::getGraph))
            .ignore(field(Component::getOverrideSettings))
            .set(field(AbstractModel::getModelType), "Component")
            .set(field(Component::getComponentType), ComponentType.TEMPLATE)
            .create();
        return component;
    }

    /**
     * Generate a Graph for testing.
     *
     * @return A dummy graph.
     */
    public Graph generateGraph() {

        var graph = Instancio
            .of(Graph.class)
            .ignore(field(AbstractModel::getId))
            .ignore(field(AbstractModel::getCreatedAt))
            .ignore(field(AbstractModel::getUpdatedAt))
            .set(field(Graph::getNodes), new ArrayList<>())
            .set(field(Graph::getComponents), new ArrayList<>())
            .set(field(Graph::getEdges), new ArrayList<>())
            .set(field(AbstractModel::getModelType), "Graph")
            .create();
        return graph;
    }

    /**
     * Generate a semantic version for testing.
     *
     * @return A semantic version.
     */
    private SemanticVersion getSemanticVersion() {
        var version = new SemanticVersion();
        version.setMajor(1);
        version.setMinor(2);
        version.setPatch(3);
        return version;
    }
}
