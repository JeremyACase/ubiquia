package org.ubiquia.common.model.ubiquia.enums;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ComponentType {

    NONE("NONE"),

    TEMPLATE("TEMPLATE"),

    POD("POD");

    private String value;

    ComponentType(String value) {
        this.value = value;
    }

    /**
     * Method to map from a string to an Enum.
     *
     * @param text The string value to map from.
     * @return The enum value of the string.
     */
    @JsonCreator
    public static ComponentType fromValue(String text) {
        for (var type : ComponentType.values()) {
            if (String.valueOf(type.value).equals(text)) {
                return type;
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
