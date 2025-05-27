package org.ubiquia.common.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EgressType {

    NONE("NONE"),

    SYNCHRONOUS("SYNCHRONOUS"),

    ASYNCHRONOUS("ASYNCHRONOUS");

    private String value;

    EgressType(String value) {
        this.value = value;
    }

    /**
     * Method to map from a string to an Enum.
     *
     * @param text The string value to map from.
     * @return The enum value of the string.
     */
    @JsonCreator
    public static EgressType fromValue(String text) {
        for (EgressType b : EgressType.values()) {
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
