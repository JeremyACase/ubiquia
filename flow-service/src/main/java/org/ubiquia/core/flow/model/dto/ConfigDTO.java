package org.ubiquia.core.flow.model.dto;


import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public class ConfigDTO {

    private Object configMap;

    private String configMountPath;

    @NotNull
    public Object getConfigMap() {
        return configMap;
    }

    public void setConfigMap(Object configMap) {
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
