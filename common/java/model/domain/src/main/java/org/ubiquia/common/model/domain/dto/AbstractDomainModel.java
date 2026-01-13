package org.ubiquia.common.model.domain.dto;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Transient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;
import org.ubiquia.common.model.domain.embeddable.KeyValuePair;

public abstract class AbstractDomainModel {

    private String ubiquiaId = null;

    private OffsetDateTime ubiquiaCreatedAt = null;

    private OffsetDateTime ubiquiaUpdatedAt = null;

    @Transient
    private String modelType;

    @ElementCollection
    @Valid
    private Set<KeyValuePair> ubiquiaTags = null;

    @Pattern(regexp = "[a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8}")
    public String getUbiquiaId() {
        return this.ubiquiaId;
    }

    public void setUbiquiaId(String ubiquiaId) {
        this.ubiquiaId = ubiquiaId;
    }

    @Valid
    public OffsetDateTime getUbiquiaCreatedAt() {
        return ubiquiaCreatedAt;
    }

    public void setUbiquiaCreatedAt(OffsetDateTime ubiquiaCreatedAt) {
        this.ubiquiaCreatedAt = ubiquiaCreatedAt;
    }

    @Valid
    public OffsetDateTime getUbiquiaUpdatedAt() {
        return ubiquiaUpdatedAt;
    }

    public void setUbiquiaUpdatedAt(OffsetDateTime ubiquiaUpdatedAt) {
        this.ubiquiaUpdatedAt = ubiquiaUpdatedAt;
    }

    @NotNull
    public String getModelType() {
        return "AbstractAclModel";
    }

    @Override
    public int hashCode() {
        return Objects.hash(ubiquiaId, modelType);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractDomainModel aentity = (AbstractDomainModel) o;

        return
            Objects.equals(this.ubiquiaId, aentity.ubiquiaId)
                && Objects.equals(this.modelType, aentity.modelType);
    }


    @Valid
    public Set<KeyValuePair> getUbiquiaTags() {
        return ubiquiaTags;
    }

    public void setUbiquiaTags(Set<KeyValuePair> ubiquiaTags) {
        this.ubiquiaTags = ubiquiaTags;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }
}
