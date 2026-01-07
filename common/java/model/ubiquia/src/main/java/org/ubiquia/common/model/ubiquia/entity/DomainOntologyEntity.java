package org.ubiquia.common.model.ubiquia.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;

@Entity
public class DomainOntologyEntity extends AbstractModelEntity {

    private String author;

    private String description;

    private String name;

    @OneToMany(
        mappedBy = "domainOntology",
        fetch = FetchType.LAZY,
        cascade = CascadeType.REFRESH)
    private List<GraphEntity> graphs;

    @OneToOne(mappedBy = "domainOntology", optional = false)
    private DomainDataContractEntity domainDataContract;

    private SemanticVersion version;

    @Override
    public String getModelType() {
        return "DomainOntology";
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<GraphEntity> getGraphs() {
        return graphs;
    }

    public void setGraphs(List<GraphEntity> graphs) {
        this.graphs = graphs;
    }

    @NotNull
    public SemanticVersion getVersion() {
        return version;
    }

    public void setVersion(SemanticVersion version) {
        this.version = version;
    }

    public DomainDataContractEntity getDomainDataContract() {
        return domainDataContract;
    }

    public void setDomainDataContract(DomainDataContractEntity domainDataContract) {
        this.domainDataContract = domainDataContract;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
