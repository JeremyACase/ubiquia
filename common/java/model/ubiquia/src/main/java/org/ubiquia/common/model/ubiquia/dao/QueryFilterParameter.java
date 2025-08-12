package org.ubiquia.common.model.ubiquia.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * A parameter used by a filter to define a constraint when querying for data.
 */
@Validated
public class QueryFilterParameter {
    @JsonProperty("key")
    private String key = "id";

    @JsonProperty("value")
    private String value = null;

    @JsonProperty("operator")
    private QueryOperatorType operator = null;

    /**
     * Get key.
     *
     * @return key
     **/
    @NotNull
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Get value.
     *
     * @return value
     **/
    @NotNull
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Get operator.
     *
     * @return operator
     **/
    @NotNull
    @Valid
    public QueryOperatorType getOperator() {
        return operator;
    }

    public void setOperator(QueryOperatorType operator) {
        this.operator = operator;
    }
}
