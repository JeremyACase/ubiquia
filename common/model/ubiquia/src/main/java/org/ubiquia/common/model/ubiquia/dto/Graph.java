package org.ubiquia.common.model.ubiquia.dto;


import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.ubiquia.common.model.ubiquia.embeddable.NameAndVersionPair;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;

public class Graph extends AbstractModel {

    private String name;

    private String author;

    private String description;

    private List<String> capabilities;

    private NameAndVersionPair agentCommunicationLanguage;

    private List<GraphEdge> edges;

    private SemanticVersion version;

    private List<Adapter> adapters;

    private List<Adapter> componentlessAdapters;

    private List<Component> components;

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

    public List<Adapter> getAdapters() {
        return adapters;
    }

    public void setAdapters(List<Adapter> adapters) {
        this.adapters = adapters;
    }

    public List<Component> getComponents() {
        return components;
    }

    public void setComponents(List<Component> components) {
        this.components = components;
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

    public List<Adapter> getComponentlessAdapters() {
        return componentlessAdapters;
    }

    public void setComponentlessAdapters(List<Adapter> componentlessAdapters) {
        this.componentlessAdapters = componentlessAdapters;
    }

    @NotNull
    public NameAndVersionPair getAgentCommunicationLanguage() {
        return agentCommunicationLanguage;
    }

    public void setAgentCommunicationLanguage(NameAndVersionPair agentCommunicationLanguage) {
        this.agentCommunicationLanguage = agentCommunicationLanguage;
    }
}

