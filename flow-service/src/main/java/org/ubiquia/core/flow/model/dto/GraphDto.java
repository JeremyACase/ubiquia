package org.ubiquia.core.flow.model.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.ubiquia.core.flow.model.embeddable.NameAndVersionPair;
import org.ubiquia.core.flow.model.embeddable.SemanticVersion;

@Validated
public class GraphDto extends AbstractEntityDto {

    @JsonProperty("graphName")
    private String graphName;

    @JsonProperty("author")
    private String author;

    @JsonProperty("description")
    private String description;

    @JsonProperty("capabilities")
    private List<String> capabilities;

    @JsonProperty("domainOntology")
    private NameAndVersionPair domainOntology;

    @JsonProperty("edges")
    private List<GraphEdgeDto> edges;

    @JsonProperty("version")
    private SemanticVersion version;

    @JsonProperty("adapters")
    private List<AdapterDto> adapters;

    @JsonProperty("dataTransformlessAdapters")
    private List<AdapterDto> dataTransformlessAdapters;

    @JsonProperty("dataTransforms")
    private List<AgentDto> dataTransforms;

    @Override
    public String getModelType() {
        return "Graph";
    }

    @NotNull
    public String getGraphName() {
        return graphName;
    }

    public void setGraphName(String graphName) {
        this.graphName = graphName;
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
    public SemanticVersion getVersion() {
        return version;
    }

    public void setVersion(SemanticVersion version) {
        this.version = version;
    }

    public List<AdapterDto> getAdapters() {
        return adapters;
    }

    public void setAdapters(List<AdapterDto> adapters) {
        this.adapters = adapters;
    }

    public List<AgentDto> getDataTransforms() {
        return dataTransforms;
    }

    public void setDataTransforms(List<AgentDto> dataTransforms) {
        this.dataTransforms = dataTransforms;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }

    public List<GraphEdgeDto> getEdges() {
        return edges;
    }

    public void setEdges(List<GraphEdgeDto> edges) {
        this.edges = edges;
    }

    public List<AdapterDto> getDataTransformlessAdapters() {
        return dataTransformlessAdapters;
    }

    public void setDataTransformlessAdapters(List<AdapterDto> dataTransformlessAdapters) {
        this.dataTransformlessAdapters = dataTransformlessAdapters;
    }

    @NotNull
    public NameAndVersionPair getDomainOntology() {
        return domainOntology;
    }

    public void setDomainOntology(NameAndVersionPair domainOntology) {
        this.domainOntology = domainOntology;
    }
}

