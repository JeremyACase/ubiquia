package org.ubiquia.common.models.embeddable;


import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

@Embeddable
public class GraphDeployment {

    private String name;

    private GraphSettings graphSettings;

    private SemanticVersion version;

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NotNull
    public SemanticVersion getVersion() {
        return version;
    }

    public void setVersion(SemanticVersion version) {
        this.version = version;
    }

    public GraphSettings getGraphSettings() {
        return graphSettings;
    }

    public void setGraphSettings(GraphSettings graphSettings) {
        this.graphSettings = graphSettings;
    }
}
