package org.ubiquia.common.model.ubiquia.dto;


import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;

public class DomainOntology extends AbstractModel {

    private String name;

    private List<Graph> graphs;

    private DomainDataContract domainDataContract;

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

    public List<Graph> getGraphs() {
        return graphs;
    }

    public void setGraphs(List<Graph> graphs) {
        this.graphs = graphs;
    }

    @NotNull
    public SemanticVersion getVersion() {
        return version;
    }

    public void setVersion(SemanticVersion version) {
        this.version = version;
    }

    public DomainDataContract getDomainDataContract() {
        return domainDataContract;
    }

    public void setDomainDataContract(DomainDataContract domainDataContract) {
        this.domainDataContract = domainDataContract;
    }
}
