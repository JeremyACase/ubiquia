package org.ubiquia.common.model.ubiquia.embeddable;


import jakarta.persistence.Embeddable;
import java.util.List;

@Embeddable
public class Cardinality {

    private List<CardinalitySetting> componentSettings;

    public List<CardinalitySetting> getComponentSettings() {
        return componentSettings;
    }

    public void setComponentSettings(List<CardinalitySetting> componentSettings) {
        this.componentSettings = componentSettings;
    }
}
