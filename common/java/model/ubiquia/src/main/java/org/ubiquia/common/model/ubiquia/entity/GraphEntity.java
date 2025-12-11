package org.ubiquia.common.model.ubiquia.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Entity
public class GraphEntity extends AbstractModelEntity {

    private String name;

    private String author;

    private List<String> capabilities;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_ontology_graph_join_id", nullable = false)
    private DomainOntologyEntity domainOntology;

    @OneToMany(mappedBy = "graph", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private Set<NodeEntity> nodes;

    @OneToMany(mappedBy = "graph", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private Set<ComponentEntity> components;
    @OneToMany(mappedBy = "graph", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private Set<FlowEntity> flows;
    @ManyToMany(mappedBy = "deployedGraphs", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<AgentEntity> agentsDeployingGraph;

    public Set<FlowEntity> getFlows() {
        return flows;
    }

    public void setFlows(HashSet<FlowEntity> flows) {
        this.flows = flows;
    }

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
    public Set<NodeEntity> getNodes() {
        return nodes;
    }

    public void setNodes(HashSet<NodeEntity> nodes) {
        this.nodes = nodes;
    }

    @NotNull
    public Set<ComponentEntity> getComponents() {
        return components;
    }

    public void setComponents(HashSet<ComponentEntity> components) {
        this.components = components;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }

    public List<AgentEntity> getAgentsDeployingGraph() {
        return agentsDeployingGraph;
    }

    public void setAgentsDeployingGraph(List<AgentEntity> agentsDeployingGraph) {
        this.agentsDeployingGraph = agentsDeployingGraph;
    }

    @NotNull
    public DomainOntologyEntity getDomainOntology() {
        return domainOntology;
    }

    public void setDomainOntology(DomainOntologyEntity domainOntology) {
        this.domainOntology = domainOntology;
    }
}

