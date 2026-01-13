package org.ubiquia.common.model.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.ubiquia.common.model.domain.embeddable.KeyValuePair;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class AbstractDomainModelEntity {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String ubiquiaId = null;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime ubiquiaCreatedAt = null;

    @UpdateTimestamp
    private OffsetDateTime ubiquiaUpdatedAt = null;

    @ElementCollection
    @Valid
    private Set<KeyValuePair> ubiquiaTags = null;

    @Transient
    private String modelType;

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

    /**
     * Type of the Model.
     *
     * @return modelType
     **/
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
        AbstractDomainModelEntity aentity = (AbstractDomainModelEntity) o;

        return
            Objects.equals(this.ubiquiaId, aentity.ubiquiaId)
                && Objects.equals(this.modelType, aentity.modelType);
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public Set<KeyValuePair> getUbiquiaTags() {
        return ubiquiaTags;
    }

    public void setUbiquiaTags(Set<KeyValuePair> ubiquiaTags) {
        this.ubiquiaTags = ubiquiaTags;
    }
}
