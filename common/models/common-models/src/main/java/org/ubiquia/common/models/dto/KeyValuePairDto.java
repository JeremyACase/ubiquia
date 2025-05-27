package org.ubiquia.common.models.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public class KeyValuePairDto {

    @JsonProperty("key")
    private String key = null;

    @JsonProperty("value")
    private Object value = null;

    @Schema(required = true)
    @NotNull
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Schema
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
