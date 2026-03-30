package org.ubiquia.common.model.ubiquia.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
public class SyncEntity {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_sync_join_id", nullable = false)
    private AbstractModelEntity model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sync_agent_join_id", nullable = false)
    private AgentEntity agent;

    @Transient
    private String modelType = "Sync";

    @Pattern(regexp = "[a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8}")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public AgentEntity getAgent() {
        return agent;
    }

    public void setAgent(AgentEntity agent) {
        this.agent = agent;
    }

    public AbstractModelEntity getModel() {
        return model;
    }

    public void setModel(AbstractModelEntity model) {
        this.model = model;
    }
}
