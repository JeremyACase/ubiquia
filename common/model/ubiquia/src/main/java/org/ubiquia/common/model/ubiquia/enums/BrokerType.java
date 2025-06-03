package org.ubiquia.common.model.ubiquia.enums;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BrokerType {

    NONE("NONE"),

    KAFKA("KAFKA");

    private String value;

    BrokerType(String value) {
        this.value = value;
    }

    /**
     * Method to map from a string to an Enum.
     *
     * @param text The string value to map from.
     * @return The enum value of the string.
     */
    @JsonCreator
    public static BrokerType fromValue(String text) {
        for (BrokerType b : BrokerType.values()) {
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
