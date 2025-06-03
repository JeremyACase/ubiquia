package org.ubiquia.common.model.ubiquia.dto;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

public abstract class BaseDomainEntityDto {

    private String id = null;

    private OffsetDateTime createdAt = null;

    private OffsetDateTime updatedAt = null;

    private String modelType;

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
        BaseDomainEntityDto aentity = (BaseDomainEntityDto) o;

        return
            Objects.equals(this.id, aentity.id)
                && Objects.equals(this.modelType, aentity.modelType);
    }
}
