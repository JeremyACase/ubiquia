package org.ubiquia.common.model.ubiquia;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.annotation.ReadOnlyProperty;

public class IngressResponse {

    private String id = null;

    private String modelType = "IngressResponse";

    private String payloadModelType = "IngressResponse";

    public IngressResponse id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Get id.
     *
     * @return id
     **/
    @Pattern(regexp = "[a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8}")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public IngressResponse modelType(String modelType) {
        this.modelType = modelType;
        return this;
    }

    /**
     * Type of the Model.
     *
     * @return modelType
     **/
    @NotNull
    @ReadOnlyProperty
    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public String getPayloadModelType() {
        return payloadModelType;
    }

    public void setPayloadModelType(String payloadModelType) {
        this.payloadModelType = payloadModelType;
    }
}
