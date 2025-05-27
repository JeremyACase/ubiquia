package org.ubiquia.common.models.dto;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.ubiquia.common.models.embeddable.KeyValuePair;

@Schema(
    description = "An abstract model that is intended to be persisted in the database.")
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "modelType",
    visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AdapterDto.class, name = "Adapter"),
    @JsonSubTypes.Type(value = FlowEventDto.class, name = "FlowEvent"),
    @JsonSubTypes.Type(value = FlowMessageDto.class, name = "FlowMessage"),
    @JsonSubTypes.Type(value = AgentDto.class, name = "Agent"),
    @JsonSubTypes.Type(value = AgentCommunicationLanguageDto.class, name = "AgentCommunicationLanguage"),
    @JsonSubTypes.Type(value = GraphDto.class, name = "Graph"),
})
public abstract class AbstractEntityDto {

    private String id = null;

    private OffsetDateTime createdAt = null;

    private OffsetDateTime updatedAt = null;

    private List<KeyValuePair> tags = null;

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
        return "AEntity";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractEntityDto aentity = (AbstractEntityDto) o;

        return
            Objects.equals(this.id, aentity.id)
                && Objects.equals(this.modelType, aentity.modelType);
    }
}
