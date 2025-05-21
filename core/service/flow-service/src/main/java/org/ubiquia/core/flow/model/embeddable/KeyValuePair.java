package org.ubiquia.core.flow.model.embeddable;


import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

import org.springframework.validation.annotation.Validated;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(
    description = "A model defining a key-value-pair that can be persisted in the database.")
@Validated
@Embeddable
public class KeyValuePair {

    @JsonProperty("key")
    @Column(name = "pair_key")
    private String key = null;

    @JsonProperty("value")
    @Column(name = "pair_value")
    private String value = null;

    /**
     * Empty constructor.
     */
    public KeyValuePair() {

    }

    /**
     * Convenience constructor.
     *
     * @param key   The key to set.
     * @param value The value to set.
     */
    public KeyValuePair(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    @ApiModelProperty(required = true, value = "key")
    @NotNull
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @ApiModelProperty(value = "value")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.key, this.value);
    }

    @Override
    public boolean equals(java.lang.Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KeyValuePair kvp = (KeyValuePair) o;

        return
            Objects.equals(this.key, kvp.key)
                && Objects.equals(this.value, kvp.value);
    }
}
