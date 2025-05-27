package org.ubiquia.common.models.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.ubiquia.common.models.embeddable.KeyValuePair;
import org.ubiquia.common.models.embeddable.SemanticVersion;


@Entity
public class Graph extends AbstractEntity {

    private String graphName;

    private String author;

    private List<String> capabilities;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acl_graph_join_id", nullable = false)
    private AgentCommunicationLanguage agentCommunicationLanguage;

    private List<KeyValuePair> tags;

    private SemanticVersion version;

    @OneToMany(mappedBy = "graph", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private List<Adapter> adapters;

    @OneToMany(mappedBy = "graph", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
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

    @NotNull
    public List<Adapter> getAdapters() {
        return adapters;
    }

    public void setAdapters(List<Adapter> adapters) {
        this.adapters = adapters;
    }

    @NotNull
    public List<Agent> getAgents() {
        return agents;
    }

    public void setAgents(List<Agent> agents) {
        this.agents = agents;
    }

    @NotNull
    public AgentCommunicationLanguage getAgentCommunicationLanguage() {
        return agentCommunicationLanguage;
    }

    public void setAgentCommunicationLanguage(AgentCommunicationLanguage agentCommunicationLanguage) {
        this.agentCommunicationLanguage = agentCommunicationLanguage;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }
}

