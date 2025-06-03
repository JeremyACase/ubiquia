package org.ubiquia.common.model.ubiquia.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AdapterType {

    EGRESS("EGRESS"),

    HIDDEN("HIDDEN"),

    MERGE("MERGE"),

    POLL("POLL"),

    PUBLISH("PUBLISH"),

    PUSH("PUSH"),

    SUBSCRIBE("SUBSCRIBE"),

    QUEUE("QUEUE");

    private String value;

    AdapterType(String value) {
        this.value = value;
    }

    /**
     * Method to map from a string to an Enum.
     *
     * @param text The string value to map from.
     * @return The enum value of the string.
     */
    @JsonCreator
    public static AdapterType fromValue(String text) {
        for (AdapterType b : AdapterType.values()) {
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
