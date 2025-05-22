package org.ubiquia.common.models.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public class KeyValuePairDto {

    @JsonProperty("key")
    private String key = null;

    @JsonProperty("value")
    private Object value = null;

    @ApiModelProperty(required = true, value = "key")
    @NotNull
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @ApiModelProperty(value = "value")
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
