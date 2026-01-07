package org.ubiquia.common.model.ubiquia.dto;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.List;
import org.ubiquia.common.model.ubiquia.embeddable.KeyValuePair;

@Schema(
    description = "An abstract model that is intended to be persisted in the database.")
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "modelType",
    visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Node.class, name = "Node"),
    @JsonSubTypes.Type(value = FlowEvent.class, name = "FlowEvent"),
    @JsonSubTypes.Type(value = FlowMessage.class, name = "FlowMessage"),
    @JsonSubTypes.Type(value = Component.class, name = "Component"),
    @JsonSubTypes.Type(value = DomainOntology.class, name = "DomainOntology"),
    @JsonSubTypes.Type(value = DomainDataContract.class, name = "DomainDataContract"),
    @JsonSubTypes.Type(value = ObjectMetadata.class, name = "ObjectMetadata"),
    @JsonSubTypes.Type(value = Graph.class, name = "Graph"),
})
public abstract class AbstractModel {

    private String id = null;

    private OffsetDateTime createdAt = null;

    private OffsetDateTime updatedAt = null;

    private List<KeyValuePair> tags = null;

    @Transient
    private String modelType;

    @Pattern(regexp = "[a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8}")
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Schema(
        example = "2018-01-01T16:00:00.123456Z",
        readOnly = true)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Schema(
        example = "2018-01-01T16:00:00.123456Z",
        readOnly = true)
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<KeyValuePair> getTags() {
        return this.tags;
    }

    public void setTags(List<KeyValuePair> tags) {
        this.tags = tags;
    }

    @Schema(required = true)
    @NotNull
    public String getModelType() {
        return "AbstractEntity";
    }
}
