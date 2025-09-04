package org.ubiquia.common.model.ubiquia.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.ubiquia.common.model.ubiquia.entity.ObjectMetadataEntity;

public class UbiquiaAgent {

    private String id = null;

    private String modelType = "UbiquiaAgent";

    private OffsetDateTime createdAt = null;

    private OffsetDateTime updatedAt = null;

    private List<Graph> deployedGraphs;

    private Set<ObjectMetadataEntity> objectMetadatas;

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

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @NotNull
    public List<Graph> getDeployedGraphs() {
        return deployedGraphs;
    }

    public void setDeployedGraphs(List<Graph> deployedGraphs) {
        this.deployedGraphs = deployedGraphs;
    }

    public String getModelType() {
        return modelType;
    }

    public Set<ObjectMetadataEntity> getObjectMetadatas() {
        return objectMetadatas;
    }

    public void setObjectMetadatas(Set<ObjectMetadataEntity> objectMetadatas) {
        this.objectMetadatas = objectMetadatas;
    }
}
