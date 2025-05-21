package org.ubiquia.core.flow.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum HttpOutputType {

    NONE("NONE"),

    POST("POST"),

    PUT("PUT");

    private String value;

    HttpOutputType(String value) {
        this.value = value;
    }

    /**
     * Method to map from a string to an Enum.
     *
     * @param text The string value to map from.
     * @return The enum value of the string.
     */
    @JsonCreator
    public static HttpOutputType fromValue(String text) {
        for (HttpOutputType b : HttpOutputType.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        return null;
    }

    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(value);
    }
}
