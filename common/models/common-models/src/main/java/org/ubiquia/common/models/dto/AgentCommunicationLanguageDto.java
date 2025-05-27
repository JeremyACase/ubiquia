package org.ubiquia.common.models.dto;


import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.ubiquia.common.models.embeddable.SemanticVersion;

public class AgentCommunicationLanguageDto extends AbstractEntityDto {

    private String domain;

    private List<GraphDto> graphs;

    private SemanticVersion version;

    private Object jsonSchema;

    @Override
    public String getModelType() {
        return "DomainOntology";
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public List<GraphDto> getGraphs() {
        return this.graphs;
    }

    public void setGraphs(List<GraphDto> graphs) {
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
