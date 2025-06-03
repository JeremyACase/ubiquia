package org.ubiquia.common.library.dao.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dao.QueryFilterParameter;

@Service
public class NonNestedPredicateBuilder {

    /**
     * Because even the help needs help.
     *
     * @param criteriaBuilder The criteria builder.
     * @param root            The root query.
     * @param parameter       The parameter we're using to build a predicate from.
     * @return A predicate.
     */
    public Predicate getPredicateHelperDate(
        final CriteriaBuilder criteriaBuilder,
        final Root<?> root,
        final QueryFilterParameter parameter) {

        Predicate predicate = null;
        var value = OffsetDateTime.parse(parameter.getValue());

        switch (parameter.getOperator()) {

            case EQUAL: {
                predicate = criteriaBuilder.equal(root.get(parameter.getKey()), value);
            }
            break;

            case LESS_THAN: {
                predicate = criteriaBuilder.lessThan(root.get(parameter.getKey()), value);
            }
            break;

            case LESS_THAN_OR_EQUAL_TO: {
                predicate = criteriaBuilder.lessThanOrEqualTo(root.get(parameter.getKey()), value);
            }
            break;

            case GREATER_THAN: {
                predicate = criteriaBuilder.greaterThan(root.get(parameter.getKey()), value);
            }
            break;

            case GREATER_THAN_OR_EQUAL_TO: {
                predicate = criteriaBuilder.greaterThanOrEqualTo(root.get(parameter.getKey()),
                    value);
            }
            break;

            default: {
                throw new IllegalArgumentException("ERROR: Unsupported operator type for "
                    + "datetime: "
                    + parameter.getOperator());
            }
        }
        return predicate;
    }

    /**
     * Because even the help needs help.
     *
     * @param criteriaBuilder The criteria builder.
     * @param root            The root query.
     * @param parameter       The parameter we're using to build a predicate from.
     * @return A predicate.
     */
    public Predicate getPredicateHelperInteger(
        final CriteriaBuilder criteriaBuilder,
        final Root<?> root,
        final QueryFilterParameter parameter) {

        Predicate predicate = null;

        Integer value = null;
        value = Integer.parseInt(parameter.getValue());
        switch (parameter.getOperator()) {

            case EQUAL: {
                predicate = criteriaBuilder.equal(root.get(parameter.getKey()), value);
            }
            break;

            case LESS_THAN: {
                predicate = criteriaBuilder.lessThan(root.get(parameter.getKey()), value);
            }
            break;

            case LESS_THAN_OR_EQUAL_TO: {
                predicate = criteriaBuilder.lessThanOrEqualTo(root.get(parameter.getKey()), value);
            }
            break;

            case GREATER_THAN: {
                predicate = criteriaBuilder.greaterThan(root.get(parameter.getKey()), value);
            }
            break;

            case GREATER_THAN_OR_EQUAL_TO: {
                predicate = criteriaBuilder.greaterThanOrEqualTo(root.get(parameter.getKey()),
                    value);
            }
            break;

            default: {
                throw new IllegalArgumentException("ERROR: Unsupported operator type for "
                    + "Integer: "
                    + parameter.getOperator());
            }
        }

        return predicate;
    }

    /**
     * Because even the help needs help.
     *
     * @param criteriaBuilder The criteria builder.
     * @param root            The root query.
     * @param parameter       The parameter we're using to build a predicate from.
     * @return A predicate.
     */
    public Predicate getPredicateHelperFloat(
        final CriteriaBuilder criteriaBuilder,
        final Root<?> root,
        final QueryFilterParameter parameter) {

        Predicate predicate = null;

        Float value = null;
        value = Float.valueOf(parameter.getValue());
        switch (parameter.getOperator()) {

            case EQUAL: {
                predicate = criteriaBuilder.equal(root.get(parameter.getKey()), value);
            }
            break;

            case LESS_THAN: {
                predicate = criteriaBuilder.lessThan(root.get(parameter.getKey()), value);
            }
            break;

            case LESS_THAN_OR_EQUAL_TO: {
                predicate = criteriaBuilder.lessThanOrEqualTo(root.get(parameter.getKey()), value);
            }
            break;

            case GREATER_THAN: {
                predicate = criteriaBuilder.greaterThan(root.get(parameter.getKey()), value);
            }
            break;

            case GREATER_THAN_OR_EQUAL_TO: {
                predicate = criteriaBuilder.greaterThanOrEqualTo(root.get(parameter.getKey()),
                    value);
            }
            break;

            default: {
                throw new IllegalArgumentException("ERROR: Unsupported operator type for "
                    + "Float: "
                    + parameter.getOperator());
            }
        }
        return predicate;
    }

    /**
     * Because even the help needs help.
     *
     * @param criteriaBuilder The criteria builder.
     * @param root            The root query.
     * @param parameter       The parameter we're using to build a predicate from.
     * @return A predicate.
     */
    public Predicate getPredicateHelperDouble(
        final CriteriaBuilder criteriaBuilder,
        final Root<?> root,
        final QueryFilterParameter parameter) {

        Predicate predicate = null;

        Double value = null;
        value = Double.valueOf(parameter.getValue());
        switch (parameter.getOperator()) {

            case EQUAL: {
                predicate = criteriaBuilder.equal(root.get(parameter.getKey()), value);
            }
            break;

            case LESS_THAN: {
                predicate = criteriaBuilder.lessThan(root.get(parameter.getKey()), value);
            }
            break;

            case LESS_THAN_OR_EQUAL_TO: {
                predicate = criteriaBuilder.lessThanOrEqualTo(root.get(parameter.getKey()), value);
            }
            break;

            case GREATER_THAN: {
                predicate = criteriaBuilder.greaterThan(root.get(parameter.getKey()), value);
            }
            break;

            case GREATER_THAN_OR_EQUAL_TO: {
                predicate = criteriaBuilder.greaterThanOrEqualTo(root.get(parameter.getKey()),
                    value);
            }
            break;

            default: {
                throw new IllegalArgumentException("ERROR: Unsupported operator type for "
                    + "Double: "
                    + parameter.getOperator());
            }
        }
        return predicate;
    }

    /**
     * Because even the help needs help.
     *
     * @param criteriaBuilder The criteria builder.
     * @param root            The root query.
     * @param field           The field we're casting.
     * @param parameter       The parameter we're using to build a predicate from.
     * @return A predicate.
     */
    public Predicate getPredicateHelperEnum(
        final CriteriaBuilder criteriaBuilder,
        final Root<?> root,
        final Field field,
        final QueryFilterParameter parameter) {

        Predicate predicate = null;

        Class clazz = field.getType();
        var value = Enum.valueOf(
            clazz,
            parameter.getValue().toUpperCase()); // Java Enums are capitalized

        switch (parameter.getOperator()) {

            case EQUAL: {
                predicate = criteriaBuilder.equal(
                    root.get(parameter.getKey()),
                    field.getType().cast(value)); // Cast type to generic run-time enum class
            }
            break;

            default: {
                throw new IllegalArgumentException("ERROR: Unsupported operator type: "
                    + parameter.getOperator());
            }
        }
        return predicate;
    }

    /**
     * Because even the help needs help.
     *
     * @param criteriaBuilder The criteria builder.
     * @param root            The root query.
     * @param parameter       The parameter we're using to build a predicate from.
     * @return A predicate.
     */
    public Predicate getPredicateHelperBoolean(
        final CriteriaBuilder criteriaBuilder,
        final Root<?> root,
        final QueryFilterParameter parameter) {

        Predicate predicate = null;

        var value = Boolean.parseBoolean(parameter.getValue());

        switch (parameter.getOperator()) {

            case EQUAL: {
                predicate = criteriaBuilder.equal(root.get(parameter.getKey()), value);
            }
            break;

            default: {
                throw new IllegalArgumentException("ERROR: Unsupported operator type: "
                    + parameter.getOperator());
            }
        }
        return predicate;
    }

    /**
     * Because even the help needs help.
     *
     * @param criteriaBuilder The criteria builder.
     * @param root            The root query.
     * @param parameter       The parameter we're using to build a predicate from.
     * @return A predicate.
     */
    public Predicate getPredicateHelperString(
        final CriteriaBuilder criteriaBuilder,
        final Root<?> root,
        final QueryFilterParameter parameter) {

        Predicate predicate = null;

        String value = null;
        value = parameter.getValue();
        switch (parameter.getOperator()) {

            case EQUAL: {
                predicate = criteriaBuilder.equal(root.get(parameter.getKey()), value);
            }
            break;

            case LESS_THAN: {
                predicate = criteriaBuilder.lessThan(root.get(parameter.getKey()), value);
            }
            break;

            case LESS_THAN_OR_EQUAL_TO: {
                predicate = criteriaBuilder.lessThanOrEqualTo(root.get(parameter.getKey()),
                    value);
            }
            break;

            case GREATER_THAN: {
                predicate = criteriaBuilder.greaterThan(root.get(parameter.getKey()), value);
            }
            break;

            case GREATER_THAN_OR_EQUAL_TO: {
                predicate = criteriaBuilder.greaterThanOrEqualTo(root.get(parameter.getKey()),
                    value);
            }
            break;

            case LIKE: {
                predicate = criteriaBuilder.like(root.get(parameter.getKey()), value);
            }
            break;

            default: {
                throw new IllegalArgumentException("ERROR: Unsupported operator type for String: "
                    + parameter.getOperator());
            }
        }
        return predicate;
    }
}
