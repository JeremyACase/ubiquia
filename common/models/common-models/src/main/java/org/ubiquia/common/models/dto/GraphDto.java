package org.ubiquia.common.models.dto;


import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.ubiquia.common.models.embeddable.NameAndVersionPair;
import org.ubiquia.common.models.embeddable.SemanticVersion;

public class GraphDto extends AbstractEntityDto {

    private String graphName;

    private String author;

    private String description;

    private List<String> capabilities;

    private NameAndVersionPair agentCommunicationLanguage;

    private List<GraphEdgeDto> edges;

    private SemanticVersion version;

    private List<AdapterDto> adapters;

    private List<AdapterDto> agentlessAdapters;

    private List<AgentDto> agents;

    @Override
    public String getModelType() {
        return "Graph";
    }

    @NotNull
    public String getGraphName() {
        return graphName;
    }

    public void setGraphName(String graphName) {
        this.graphName = graphName;
    }

    @NotNull
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @NotNull
    public SemanticVersion getVersion() {
        return version;
    }

    public void setVersion(SemanticVersion version) {
        this.version = version;
    }

    public List<AdapterDto> getAdapters() {
        return adapters;
    }

    public void setAdapters(List<AdapterDto> adapters) {
        this.adapters = adapters;
    }

    public List<AgentDto> getAgents() {
        return agents;
    }

    public void setAgents(List<AgentDto> agents) {
        this.agents = agents;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }

    public List<GraphEdgeDto> getEdges() {
        return edges;
    }

    public void setEdges(List<GraphEdgeDto> edges) {
        this.edges = edges;
    }

    public List<AdapterDto> getAgentlessAdapters() {
        return agentlessAdapters;
    }

    public void setAgentlessAdapters(List<AdapterDto> agentlessAdapters) {
        this.agentlessAdapters = agentlessAdapters;
    }

    @NotNull
    public NameAndVersionPair getAgentCommunicationLanguage() {
        return agentCommunicationLanguage;
    }

    public void setAgentCommunicationLanguage(NameAndVersionPair agentCommunicationLanguage) {
        this.agentCommunicationLanguage = agentCommunicationLanguage;
    }
}

