package org.ubiquia.common.model.ubiquia.entity;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.ubiquia.common.model.ubiquia.embeddable.KeyValuePair;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class AbstractModelEntity {

    @Id
    private String id = null;

    @PrePersist
    private void generateId() {
        if (Objects.isNull(this.id)) {
            this.id = UUID.randomUUID().toString();
        }
    }

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt = null;

    @UpdateTimestamp
    private OffsetDateTime updatedAt = null;

    @ElementCollection
    @Valid
    private Set<KeyValuePair> tags = null;

    @OneToMany(mappedBy = "model", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private Set<UpdateEntity> updates;

    @OneToMany(mappedBy = "model", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private Set<SyncEntity> syncs;

    @Transient
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

    /**
     * Time the row was last updated in the database, auto-populated by the system.
     *
     * @return updatedAt
     **/
    @Valid
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * A list of tags in key-value-pair format that this record has associated with it.
     *
     * @return tags
     **/
    @Valid
    public Set<KeyValuePair> getTags() {
        return this.tags;
    }

    public void setTags(Set<KeyValuePair> tags) {
        this.tags = tags;
    }

    /**
     * Type of the Model.
     *
     * @return modelType
     **/
    @NotNull
    public String getModelType() {
        return "AbstractEntity";
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, modelType);
    }

    @Override
    public boolean equals(java.lang.Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractModelEntity aentity = (AbstractModelEntity) o;

        return
            Objects.equals(this.id, aentity.id)
                && Objects.equals(this.modelType, aentity.modelType);
    }

    public Set<UpdateEntity> getUpdates() {
        return updates;
    }

    public void setUpdates(Set<UpdateEntity> updates) {
        this.updates = updates;
    }

    public Set<SyncEntity> getSyncs() {
        return syncs;
    }

    public void setSyncs(Set<SyncEntity> syncs) {
        this.syncs = syncs;
    }
}
