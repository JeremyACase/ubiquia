package org.ubiquia.common.model.ubiquia.dto;


import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.ubiquia.common.model.ubiquia.embeddable.NameAndVersionPair;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;

public class Graph extends AbstractModel {

    private String graphName;

    private String author;

    private String description;

    private List<String> capabilities;

    private NameAndVersionPair agentCommunicationLanguage;

    private List<GraphEdge> edges;

    private SemanticVersion version;

    private List<Adapter> adapters;

    private List<Adapter> agentlessAdapters;

    private List<Agent> agents;

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

    public List<Adapter> getAdapters() {
        return adapters;
    }

    public void setAdapters(List<Adapter> adapters) {
        this.adapters = adapters;
    }

    public List<Agent> getAgents() {
        return agents;
    }

    public void setAgents(List<Agent> agents) {
        this.agents = agents;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }

    public List<GraphEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<GraphEdge> edges) {
        this.edges = edges;
    }

    public List<Adapter> getAgentlessAdapters() {
        return agentlessAdapters;
    }

    public void setAgentlessAdapters(List<Adapter> agentlessAdapters) {
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

