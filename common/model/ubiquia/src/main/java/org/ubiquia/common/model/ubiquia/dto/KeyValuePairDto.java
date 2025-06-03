package org.ubiquia.common.model.ubiquia.dto;


import jakarta.validation.constraints.NotNull;

public class KeyValuePairDto {

    private String key = null;

    private Object value = null;

    @NotNull
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
