package org.ubiquia.common.model.ubiquia.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;


@Entity
public class GraphEntity extends AbstractModelEntity {

    private String name;

    private String author;

    private List<String> capabilities;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acl_graph_join_id", nullable = false)
    private AgentCommunicationLanguageEntity agentCommunicationLanguage;

    private SemanticVersion version;

    @OneToMany(mappedBy = "graph", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private List<AdapterEntity> adapters;

    @OneToMany(mappedBy = "graph", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private List<ComponentEntity> components;

    @ManyToMany(mappedBy = "deployedGraphs", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<UbiquiaAgentEntity> ubiquiaAgentsDeployingGraph;

    @Override
    public String getModelType() {
        return "Graph";
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
    public List<AdapterEntity> getAdapters() {
        return adapters;
    }

    public void setAdapters(List<AdapterEntity> adapters) {
        this.adapters = adapters;
    }

    @NotNull
    public List<ComponentEntity> getComponents() {
        return components;
    }

    public void setComponents(List<ComponentEntity> components) {
        this.components = components;
    }

    @NotNull
    public AgentCommunicationLanguageEntity getAgentCommunicationLanguage() {
        return agentCommunicationLanguage;
    }

    public void setAgentCommunicationLanguage(AgentCommunicationLanguageEntity agentCommunicationLanguageEntity) {
        this.agentCommunicationLanguage = agentCommunicationLanguageEntity;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }

    public List<UbiquiaAgentEntity> getUbiquiaAgentsDeployingGraph() {
        return ubiquiaAgentsDeployingGraph;
    }

    public void setUbiquiaAgentsDeployingGraph(List<UbiquiaAgentEntity> ubiquiaAgentsDeployingGraph) {
        this.ubiquiaAgentsDeployingGraph = ubiquiaAgentsDeployingGraph;
    }
}

