package org.ubiquia.common.library.dao.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dao.QueryFilterParameter;
import org.ubiquia.common.model.ubiquia.dao.QueryOperatorType;

@Service
public class NestedPredicateBuilder {

    public Predicate getNestedQueryPredicate(
        QueryFilterParameter param,
        CriteriaBuilder cb,
        Path path,
        Subquery subQuery) {

        var value = parseValue(param.getValue(), path.getJavaType());
        return buildNestedPredicate(cb, path, subQuery, param.getOperator(), value);
    }

    public Predicate getNestedQueryPredicate(
        String keychain,
        CriteriaBuilder cb,
        Path path,
        Subquery subQuery,
        String queryValue,
        Class<?> type) {

        var value = parseValue(queryValue, type);
        var operator = getOperatorFromKeychain(keychain, queryValue);

        return buildNestedPredicate(cb, path, subQuery, operator, value);
    }

    public Predicate getNestedQueryPredicateEnum(
        CriteriaBuilder cb,
        Path path,
        Subquery subQuery,
        Field field,
        String queryValue) {

        var clazz = field.getType();
        var value = Enum.valueOf(clazz.asSubclass(Enum.class), queryValue.toUpperCase());
        if ("!null".equalsIgnoreCase(queryValue)) {
            subQuery.where(cb.isNotNull(path));
        } else if ("null".equalsIgnoreCase(queryValue)) {
            subQuery.where(cb.isNull(path));
        } else {
            subQuery.where(cb.equal(path, field.getType().cast(value)));
        }

        return cb.exists(subQuery);
    }

    private Predicate buildNestedPredicate(
        CriteriaBuilder cb,
        Path path,
        Subquery subQuery,
        QueryOperatorType operator,
        Object value) {

        if (value == null) {
            subQuery.where(cb.isNull(path));
        } else if ("!null".equalsIgnoreCase(String.valueOf(value))) {
            subQuery.where(cb.isNotNull(path));
        } else {
            switch (operator) {
                case EQUAL -> subQuery.where(cb.equal(path, value));
                case GREATER_THAN -> subQuery.where(cb.greaterThan(path, (Comparable) value));
                case GREATER_THAN_OR_EQUAL_TO -> subQuery.where(cb.greaterThanOrEqualTo(path, (Comparable) value));
                case LESS_THAN -> subQuery.where(cb.lessThan(path, (Comparable) value));
                case LESS_THAN_OR_EQUAL_TO -> subQuery.where(cb.lessThanOrEqualTo(path, (Comparable) value));
                default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
            }
        }

        return cb.exists(subQuery);
    }

    private QueryOperatorType getOperatorFromKeychain(String keychain, String value) {
        if (keychain.endsWith("<")) {
            return QueryOperatorType.LESS_THAN;
        }
        if (keychain.endsWith(">")) {
            return QueryOperatorType.GREATER_THAN;
        }
        if (keychain.endsWith("*")) {
            return QueryOperatorType.LIKE;
        }
        return QueryOperatorType.EQUAL;
    }

    private Object parseValue(String value, Class<?> type) {
        if (value == null) {
            return null;
        }
        if (type.equals(OffsetDateTime.class)) {
            return OffsetDateTime.parse(value);
        } else if (type.equals(Integer.class)) {
            return Integer.valueOf(value);
        } else if (type.equals(Float.class)) {
            return Float.valueOf(value);
        } else if (type.equals(Double.class)) {
            return Double.valueOf(value);
        } else if (type.equals(Boolean.class)) {
            return Boolean.valueOf(value);
        } else {
            return value;
        }
    }
}
