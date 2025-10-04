package org.ubiquia.common.model.ubiquia.embeddable;


import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

@Embeddable
public class CardinalitySetting {

    private String name;

    private Boolean enabled = true;

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NotNull
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
