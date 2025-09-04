package org.ubiquia.common.library.dao.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Color {
    BLACK("black"),
    BROWN("brown"),
    WHITE("white");

    private String value;

    Color(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(value);
    }
}