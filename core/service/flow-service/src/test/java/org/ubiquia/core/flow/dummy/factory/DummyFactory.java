package org.ubiquia.core.flow.dummy.factory;

import static org.instancio.Select.field;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import org.instancio.Instancio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.mock.MockRegistrar;
import org.ubiquia.common.models.dto.AbstractEntityDto;
import org.ubiquia.common.models.dto.AdapterDto;
import org.ubiquia.common.models.dto.AgentDto;
import org.ubiquia.common.models.dto.GraphDto;
import org.ubiquia.common.models.embeddable.AdapterSettings;
import org.ubiquia.common.models.embeddable.NameAndVersionPair;
import org.ubiquia.common.models.embeddable.SemanticVersion;
import org.ubiquia.common.models.embeddable.SubSchema;
import org.ubiquia.common.models.enums.AgentType;


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
    public AdapterDto generateAdapter() {
        var adapter = Instancio
            .of(AdapterDto.class)
            .ignore(field(AbstractEntityDto::getId))
            .ignore(field(AbstractEntityDto::getCreatedAt))
            .ignore(field(AbstractEntityDto::getUpdatedAt))
            .ignore(field(AdapterDto::getGraph))
            .ignore(field(AdapterDto::getBrokerSettings))
            .ignore(field(AdapterDto::getAgent))
            .ignore(field(AdapterDto::getDownstreamAdapters))
            .ignore(field(AdapterDto::getEgressSettings))
            .ignore(field(AdapterDto::getOutputSubSchema))
            .ignore(field(AdapterDto::getPollSettings))
            .ignore(field(AdapterDto::getOverrideSettings))
            .ignore(field(AdapterDto::getAdapterType))
            .set(field(AdapterDto::getAdapterSettings), new AdapterSettings())
            .set(field(AdapterDto::getFlowEvents), new ArrayList<>())
            .set(field(AdapterDto::getInputSubSchemas), new ArrayList<>())
            .set(field(AbstractEntityDto::getModelType), "Adapter")
            .create();
        return adapter;
    }

    /**
     * Generate an agent for testing.
     *
     * @return An agent.
     */
    public AgentDto generateAgent() {
        var agent = Instancio
            .of(AgentDto.class)
            .ignore(field(AbstractEntityDto::getId))
            .ignore(field(AbstractEntityDto::getCreatedAt))
            .ignore(field(AbstractEntityDto::getUpdatedAt))
            .ignore(field(AgentDto::getAdapter))
            .ignore(field(AgentDto::getConfig))
            .ignore(field(AgentDto::getEnvironmentVariables))
            .ignore(field(AgentDto::getGraph))
            .ignore(field(AgentDto::getOverrideSettings))
            .set(field(AbstractEntityDto::getModelType), "Agent")
            .set(field(AgentDto::getAgentType), AgentType.TEMPLATE)
            .create();
        return agent;
    }

    /**
     * Generate a Graph for testing.
     *
     * @return A dummy graph.
     */
    @Transactional
    public GraphDto generateGraph() throws IOException {

        var domainOntology = this.mockRegistrar.tryRegisterAcl();

        var nameAndVersionPair = new NameAndVersionPair();
        nameAndVersionPair.setName(domainOntology.getDomain());
        nameAndVersionPair.setVersion(domainOntology.getVersion());

        var graph = Instancio
            .of(GraphDto.class)
            .ignore(field(AbstractEntityDto::getId))
            .ignore(field(AbstractEntityDto::getCreatedAt))
            .ignore(field(AbstractEntityDto::getUpdatedAt))
            .set(field(GraphDto::getAdapters), new ArrayList<>())
            .set(field(GraphDto::getAgents), new ArrayList<>())
            .set(field(GraphDto::getAgentlessAdapters), new ArrayList<>())
            .set(field(GraphDto::getEdges), new ArrayList<>())
            .set(field(GraphDto::getVersion), this.getSemanticVersion())
            .set(field(GraphDto::getAgentCommunicationLanguage), nameAndVersionPair)
            .set(field(AbstractEntityDto::getModelType), "Graph")
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
