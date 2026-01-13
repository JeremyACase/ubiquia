package org.ubiquia.common.model.ubiquia.embeddable;


import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

@Embeddable
public class GraphDeployment {

    private String graphName;

    private String domainOntologyName;

    private SemanticVersion domainVersion;

    private Cardinality cardinality;

    private GraphSettings graphSettings;

    @NotNull
    public String getGraphName() {
        return graphName;
    }

    public void setGraphName(String graphName) {
        this.graphName = graphName;
    }

    public GraphSettings getGraphSettings() {
        return graphSettings;
    }

    public void setGraphSettings(GraphSettings graphSettings) {
        this.graphSettings = graphSettings;
    }

    public Cardinality getCardinality() {
        return cardinality;
    }

    public void setCardinality(Cardinality cardinality) {
        this.cardinality = cardinality;
    }

    public String getDomainOntologyName() {
        return domainOntologyName;
    }

    public void setDomainOntologyName(String domainOntologyName) {
        this.domainOntologyName = domainOntologyName;
    }

    public SemanticVersion getDomainVersion() {
        return domainVersion;
    }

    public void setDomainVersion(SemanticVersion domainVersion) {
        this.domainVersion = domainVersion;
    }
}
