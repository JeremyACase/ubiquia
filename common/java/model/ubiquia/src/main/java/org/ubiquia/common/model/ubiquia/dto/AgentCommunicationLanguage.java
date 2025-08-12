package org.ubiquia.common.model.ubiquia.dto;


import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;

public class AgentCommunicationLanguage extends AbstractModel {

    private String domain;

    private List<Graph> graphs;

    private SemanticVersion version;

    private Object jsonSchema;

    @Override
    public String getModelType() {
        return "AgentCommunicationLanguage";
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public List<Graph> getGraphs() {
        return this.graphs;
    }

    public void setGraphs(List<Graph> graphs) {
        this.graphs = graphs;
    }

    @NotNull
    public Object getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(Object jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    @NotNull
    public SemanticVersion getVersion() {
        return version;
    }

    public void setVersion(SemanticVersion version) {
        this.version = version;
    }
}
