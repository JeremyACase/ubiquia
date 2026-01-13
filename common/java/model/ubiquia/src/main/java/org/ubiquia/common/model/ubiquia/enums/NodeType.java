package org.ubiquia.common.model.ubiquia.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NodeType {

    EGRESS("EGRESS"),

    HIDDEN("HIDDEN"),

    MERGE("MERGE"),

    POLL("POLL"),

    PUBLISH("PUBLISH"),

    PUSH("PUSH"),

    SUBSCRIBE("SUBSCRIBE"),

    QUEUE("QUEUE");

    private String value;

    NodeType(String value) {
        this.value = value;
    }

    /**
     * Method to map from a string to an Enum.
     *
     * @param text The string value to map from.
     * @return The enum value of the string.
     */
    @JsonCreator
    public static NodeType fromValue(String text) {
        for (NodeType b : NodeType.values()) {
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
