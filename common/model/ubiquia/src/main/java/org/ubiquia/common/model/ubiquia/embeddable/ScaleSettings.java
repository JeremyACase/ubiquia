package org.ubiquia.common.model.ubiquia.embeddable;


import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Embeddable
public class ScaleSettings {

    private Integer minReplicas = 1;

    @NotNull
    @Min(1)
    public Integer getMinReplicas() {
        return minReplicas;
    }

    public void setMinReplicas(Integer minReplicas) {
        this.minReplicas = minReplicas;
    }
}
