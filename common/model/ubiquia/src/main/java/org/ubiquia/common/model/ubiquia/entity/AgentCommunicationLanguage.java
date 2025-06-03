package org.ubiquia.common.model.ubiquia.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;

@Entity
public class AgentCommunicationLanguage extends AbstractEntity {

    private String domain;

    @OneToMany(
        mappedBy = "agentCommunicationLanguage",
        fetch = FetchType.LAZY,
        cascade = CascadeType.REFRESH)
    private List<Graph> graphs;

    private SemanticVersion version;

    @Column(columnDefinition = "TEXT")
    private String jsonSchema;

    @Override
    public String getModelType() {
        return "AgentCommunicationLanguage";
    }

    @NotNull
    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public List<Graph> getGraphs() {
        return graphs;
    }

    public void setGraphs(List<Graph> graphList) {
        this.graphs = graphList;
    }

    public String getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(String jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    public SemanticVersion getVersion() {
        return version;
    }

    public void setVersion(SemanticVersion version) {
        this.version = version;
    }
}
