package org.ubiquia.common.model.ubiquia.embeddable;


import jakarta.persistence.Embeddable;
import java.util.List;

@Embeddable
public class Cardinality {

    private List<CardinalitySetting> componentSettings;

    private List<CardinalitySetting> componentlessAdapterSettings;

    public List<CardinalitySetting> getComponentSettings() {
        return componentSettings;
    }

    public void setComponentSettings(List<CardinalitySetting> componentSettings) {
        this.componentSettings = componentSettings;
    }

    public List<CardinalitySetting> getComponentlessAdapterSettings() {
        return componentlessAdapterSettings;
    }

    public void setComponentlessAdapterSettings(List<CardinalitySetting> componentlessAdapterSettings) {
        this.componentlessAdapterSettings = componentlessAdapterSettings;
    }
}
