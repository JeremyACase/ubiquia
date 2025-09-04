package org.ubiquia.common.model.ubiquia.dao;

/**
 * An enum defining the types of query operands clients can use.
 */
public enum QueryOperatorType {
    LESS_THAN("LESS_THAN"),
    LESS_THAN_OR_EQUAL_TO("LESS_THAN_OR_EQUAL_TO"),
    GREATER_THAN("GREATER_THAN"),
    GREATER_THAN_OR_EQUAL_TO("GREATER_THAN_OR_EQUAL_TO"),
    LIKE("LIKE"),
    EQUAL("EQUAL");

    private String value;

    QueryOperatorType(String value) {
        this.value = value;
    }
}
