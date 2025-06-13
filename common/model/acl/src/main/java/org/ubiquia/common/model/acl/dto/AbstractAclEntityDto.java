package org.ubiquia.common.model.acl.dto;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Transient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;
import org.ubiquia.common.model.acl.embeddable.KeyValuePair;

public abstract class AbstractAclEntityDto {

    private String id = null;

    private OffsetDateTime createdAt = null;

    private OffsetDateTime updatedAt = null;

    @Transient
    private String modelType;

    @ElementCollection
    @Valid
    private Set<KeyValuePair> tags = null;

    @Pattern(regexp = "[a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8}")
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Valid
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Valid
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @NotNull
    public String getModelType() {
        return "BaseDomainEntity";
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, modelType);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractAclEntityDto aentity = (AbstractAclEntityDto) o;

        return
            Objects.equals(this.id, aentity.id)
                && Objects.equals(this.modelType, aentity.modelType);
    }


    @Valid
    public Set<KeyValuePair> getTags() {
        return tags;
    }

    public void setTags(Set<KeyValuePair> tags) {
        this.tags = tags;
    }
}
