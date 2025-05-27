package org.ubiquia.common.models.embeddable;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import org.springframework.validation.annotation.Validated;

@Embeddable
public class SubSchema {

    @JsonProperty("modelName")
    private String modelName;

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}
