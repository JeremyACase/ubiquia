package org.ubiquia.common.model.ubiquia.embeddable;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

@Embeddable
public class Config {

    @Column(columnDefinition = "LONGTEXT")
    private String configMap;

    private String configMountPath;

    @NotNull
    public String getConfigMap() {
        return configMap;
    }

    public void setConfigMap(String configMap) {
        this.configMap = configMap;
    }

    @NotNull
    public String getConfigMountPath() {
        return configMountPath;
    }

    public void setConfigMountPath(String configMountPath) {
        this.configMountPath = configMountPath;
    }
}
