package org.ubiquia.common.model.ubiquia.dao;

/**
 * An enum defining the types of sorts a client can use.
 */
public enum SortType {
    ASCENDING("ASCENDING"),
    DESCENDING("DESCENDING");

    private String value;

    SortType(String value) {
        this.value = value;
    }
}