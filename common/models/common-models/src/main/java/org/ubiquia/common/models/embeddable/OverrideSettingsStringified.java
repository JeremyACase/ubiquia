package org.ubiquia.common.models.embeddable;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

@Embeddable
public class OverrideSettingsStringified {

    @Column(name = "override_settings_flag")
    private String flag;

    @Column(name = "override_settings_key")
    private String key;

    @Column(name = "override_settings_value")
    private String value;

    @NotNull
    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    @NotNull
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @NotNull
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
