package org.ubiquia.common.model.ubiquia.dto;


import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;

public class Sync {

    private String id = null;

    private OffsetDateTime createdAt = null;

    private String modelType = "Sync";

    private AbstractModel model;

    private Agent agent;

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

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public AbstractModel getModel() {
        return model;
    }

    public void setModel(AbstractModel model) {
        this.model = model;
    }
}
