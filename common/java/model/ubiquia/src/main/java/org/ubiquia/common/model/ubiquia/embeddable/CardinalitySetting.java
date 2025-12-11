package org.ubiquia.common.model.ubiquia.embeddable;


import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

@Embeddable
public class CardinalitySetting {

    private String componentName;

    private Integer replicas = 1;

    @NotNull
    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    @NotNull
    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }
}
