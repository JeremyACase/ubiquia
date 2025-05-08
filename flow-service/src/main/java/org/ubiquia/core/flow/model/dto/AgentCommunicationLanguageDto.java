package org.ubiquia.core.flow.model.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.ubiquia.core.flow.model.embeddable.SemanticVersion;

@Validated
public class AgentCommunicationLanguageDto extends AbstractEntityDto {

    @JsonProperty("domain")
    private String domain;

    @JsonProperty("graphs")
    private List<GraphDto> graphs;

    @JsonProperty("version")
    private SemanticVersion version;

    @JsonProperty("jsonSchema")
    private Object jsonSchema;

    @Override
    public String getModelType() {
        return "DomainOntology";
    }

    @NotNull
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
