package org.ubiquia.common.model.ubiquia.embeddable;


import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
public class Config {

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
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
