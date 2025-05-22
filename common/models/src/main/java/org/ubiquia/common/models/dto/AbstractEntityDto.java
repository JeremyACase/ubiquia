package org.ubiquia.common.models.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Id;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.ubiquia.common.models.embeddable.KeyValuePair;

@ApiModel(
    description = "An abstract model that is intended to be persisted in the database.")
@Validated
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

    @JsonProperty("id")
    @Id
    private String id = null;

    @JsonProperty("createdAt")
    private OffsetDateTime createdAt = null;

    @JsonProperty("updatedAt")
    private OffsetDateTime updatedAt = null;

    @JsonProperty("tags")
    @Valid
    private List<KeyValuePair> tags = null;

    @JsonProperty("modelType")
    private String modelType;

    @ApiModelProperty(readOnly = true, value = "")
    @Pattern(regexp = "[a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8}")
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(
        example = "2018-01-01T16:00:00.123456Z",
        readOnly = true,
        value = "Time the row was created in the database, auto-populated by the system")
    @Valid
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @ApiModelProperty(
        example = "2018-01-01T16:00:00.123456Z",
        readOnly = true,
        value = "Time the row was last updated in the database, auto-populated by the system")
    @Valid
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @ApiModelProperty(value = "A list of tags in key-value-pair format that this record has associated with it.")
    @Valid
    public List<KeyValuePair> getTags() {
        return this.tags;
    }

    public void setTags(List<KeyValuePair> tags) {
        this.tags = tags;
    }

    @ApiModelProperty(
        required = true,
        value = "Type of the Model")
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
