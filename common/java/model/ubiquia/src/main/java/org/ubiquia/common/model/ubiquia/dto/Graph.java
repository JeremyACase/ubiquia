package org.ubiquia.common.model.ubiquia.dto;


import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.List;

public class Graph extends AbstractModel {

    private String name;

    private String author;

    private String description;

    private List<String> capabilities;

    private List<GraphEdge> edges;

    private DomainOntology domainOntology;

    private List<Node> nodes;

    private HashSet<Flow> flows;

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

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
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

    public HashSet<Flow> getFlows() {
        return flows;
    }

    public void setFlows(HashSet<Flow> flows) {
        this.flows = flows;
    }

    public DomainOntology getDomainOntology() {
        return domainOntology;
    }

    public void setDomainOntology(DomainOntology domainOntology) {
        this.domainOntology = domainOntology;
    }
}

