package org.ubiquia.core.flow.dummy.factory;

import static org.instancio.Select.field;

import java.io.IOException;
import java.util.ArrayList;
import org.instancio.Instancio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.AbstractModel;
import org.ubiquia.common.model.ubiquia.dto.Adapter;
import org.ubiquia.common.model.ubiquia.dto.Component;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.model.ubiquia.embeddable.AdapterSettings;
import org.ubiquia.common.model.ubiquia.embeddable.NameAndVersionPair;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;
import org.ubiquia.common.model.ubiquia.embeddable.SubSchema;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;
import org.ubiquia.core.flow.mock.MockRegistrar;


@Service
public class DummyFactory {

    @Autowired
    private MockRegistrar mockRegistrar;

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
    public Adapter generateAdapter() {
        var adapter = Instancio
            .of(Adapter.class)
            .ignore(field(AbstractModel::getId))
            .ignore(field(AbstractModel::getCreatedAt))
            .ignore(field(AbstractModel::getUpdatedAt))
            .ignore(field(Adapter::getGraph))
            .ignore(field(Adapter::getBrokerSettings))
            .ignore(field(Adapter::getComponent))
            .ignore(field(Adapter::getDownstreamAdapters))
            .ignore(field(Adapter::getEgressSettings))
            .ignore(field(Adapter::getOutputSubSchema))
            .ignore(field(Adapter::getPollSettings))
            .ignore(field(Adapter::getOverrideSettings))
            .ignore(field(Adapter::getAdapterType))
            .set(field(Adapter::getAdapterSettings), new AdapterSettings())
            .set(field(Adapter::getFlowEvents), new ArrayList<>())
            .set(field(Adapter::getInputSubSchemas), new ArrayList<>())
            .set(field(AbstractModel::getModelType), "Adapter")
            .create();
        return adapter;
    }

    /**
     * Generate a component for testing.
     *
     * @return A component.
     */
    public Component generateComponent() {
        var agent = Instancio
            .of(Component.class)
            .ignore(field(AbstractModel::getId))
            .ignore(field(AbstractModel::getCreatedAt))
            .ignore(field(AbstractModel::getUpdatedAt))
            .ignore(field(Component::getAdapter))
            .ignore(field(Component::getConfig))
            .ignore(field(Component::getEnvironmentVariables))
            .ignore(field(Component::getGraph))
            .ignore(field(Component::getOverrideSettings))
            .set(field(AbstractModel::getModelType), "Component")
            .set(field(Component::getComponentType), ComponentType.TEMPLATE)
            .create();
        return agent;
    }

    /**
     * Generate a Graph for testing.
     *
     * @return A dummy graph.
     */
    public Graph generateGraph() throws IOException {

        var acl = this.mockRegistrar.tryRegisterAcl();

        var nameAndVersionPair = new NameAndVersionPair();
        nameAndVersionPair.setName(acl.getDomain());
        nameAndVersionPair.setVersion(acl.getVersion());

        var graph = Instancio
            .of(Graph.class)
            .ignore(field(AbstractModel::getId))
            .ignore(field(AbstractModel::getCreatedAt))
            .ignore(field(AbstractModel::getUpdatedAt))
            .set(field(Graph::getAdapters), new ArrayList<>())
            .set(field(Graph::getComponents), new ArrayList<>())
            .set(field(Graph::getComponentlessAdapters), new ArrayList<>())
            .set(field(Graph::getEdges), new ArrayList<>())
            .set(field(Graph::getVersion), this.getSemanticVersion())
            .set(field(Graph::getAgentCommunicationLanguage), nameAndVersionPair)
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
