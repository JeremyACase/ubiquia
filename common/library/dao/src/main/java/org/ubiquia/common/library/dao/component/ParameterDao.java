package org.ubiquia.common.library.dao.component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.OffsetDateTime;
import java.util.*;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.ubiquia.common.library.dao.service.logic.EntityDeriver;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.library.dao.service.logic.ClassDeriver;
import org.ubiquia.common.library.dao.service.builder.NestedPredicateBuilder;
import org.ubiquia.common.library.dao.service.builder.NonNestedPredicateBuilder;
import org.ubiquia.common.model.ubiquia.dao.QueryFilterParameter;
import org.ubiquia.common.model.ubiquia.dao.QueryOperatorType;


/**
 * This is a Data Acces Object (DAO) that can be used to query data from a relational database
 * for an entity type provided "params" (such as might be provided via a RESTful GET call.)
 *
 * @param <T> The entity type.
 */
@Component
public class ParameterDao<T> {

    private static final Logger logger = LoggerFactory.getLogger(ParameterDao.class);
    @Autowired
    private ClassDeriver classDeriver;
    @Autowired
    private EntityDeriver entityDeriver;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private NestedPredicateBuilder nestedPredicateBuilder;
    @Autowired
    private NonNestedPredicateBuilder nonNestedPredicateBuilder;

    /**
     * Query a page of data from the database provided a list of parameters.
     *
     * @param parameters     The parameters received to generate data from.
     * @param page           The page number to retrieve.
     * @param size           The size of the page.
     * @param sortDescending Whether or not to sort descending.
     * @param sortByFields   The fields to sort by.
     * @param clazz          The class we're querying data for.
     * @return A page of data retrieved from the database.
     * @throws NoSuchFieldException Exceptions from clients passing invalid fields.
     */
    @Transactional
    public Page<T> getPage(
        final Map<String, String[]> parameters,
        final int page,
        final int size,
        final Boolean sortDescending,
        final List<String> sortByFields,
        final Class<T> clazz) throws NoSuchFieldException {

        var keychains = new ArrayList<>(parameters.keySet());

        var predicateClass = this.classDeriver.tryGetPredicateClass(clazz, keychains);

        var criteriaBuilder = this.entityManager.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(predicateClass);
        var root = criteriaQuery.from(predicateClass);

        this.trySetOrderBy(sortByFields, sortDescending, criteriaBuilder, root, criteriaQuery);
        var pageRequest = this.getPageRequest(sortByFields, sortDescending, page, size);

        var pageResponse = this.queryDatabase(
            parameters,
            criteriaBuilder,
            criteriaQuery,
            root,
            pageRequest,
            predicateClass);

        return pageResponse;
    }

    /**
     * Return a paginated list of specifically-selected fields.
     *
     * @param parameters        The parameters received to generate data from.
     * @param page              The page number to retrieve.
     * @param size              The size of the page.
     * @param sortDescending    Whether or not to sort descending.
     * @param sortByFields      The fields to sort by.
     * @param multiSelectFields The fields we're selecting for.
     * @param clazz             The class we're querying data for.
     * @return A page of objects retrieved from the database.
     * @throws NoSuchFieldException Exception from invalid fields in client request.
     */
    @Transactional
    public Page<Object[]> getPageMultiSelect(
        final Map<String, String[]> parameters,
        final int page,
        final int size,
        final Boolean sortDescending,
        final List<String> sortByFields,
        final List<String> multiSelectFields,
        final Class<T> clazz) throws NoSuchFieldException {

        if (multiSelectFields.isEmpty()) {
            throw new IllegalArgumentException("ERROR: method requires at least "
                + "1 multiselect field...");
        }

        var keychains = new ArrayList<>(parameters.keySet());

        var predicateClass = this.classDeriver.tryGetPredicateClass(clazz, keychains);

        var criteriaBuilder = this.entityManager.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(Object[].class);
        var root = criteriaQuery.from(predicateClass);

        var selections = new ArrayList<Selection<?>>();
        for (var field : multiSelectFields) {
            var selection = root.get(field);
            selections.add(selection);
        }
        criteriaQuery.multiselect(selections);

        this.trySetOrderBy(sortByFields, sortDescending, criteriaBuilder, root, criteriaQuery);
        var pageRequest = this.getPageRequest(sortByFields, sortDescending, page, size);

        var predicates = this.getPredicates(
            parameters,
            criteriaBuilder,
            criteriaQuery,
            root,
            clazz);

        // Create a "compound predicate" - a conjunction - from all the filtered parameters.
        var conjunction = criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));
        criteriaQuery.where(conjunction);

        var listSize = pageRequest.getPageSize();
        if (listSize <= 0) {
            listSize = 1;
        }

        // Build our query results from our requested page.
        var query = this.entityManager.createQuery(criteriaQuery)
            .setFirstResult(pageRequest.getPageNumber() * pageRequest.getPageSize())
            .setHint("org.hibernate.cacheable", true)
            .setMaxResults(listSize);
        var records = query.getResultList();

        var count = this.getCount(parameters, clazz);
        var paged = new GenericPageImplementation<>(records, pageRequest, count);
        return paged;
    }

    /**
     * Query the database provided the parameters.
     *
     * @param parameters      The parameters to use as predicates.
     * @param criteriaBuilder The criteria builder.
     * @param criteriaQuery   Our JPA query.
     * @param root            The root-most class representing our table.
     * @param pageRequest     The page request.
     * @param clazz           The class we're querying data for.
     * @return A page of data.
     * @throws NoSuchFieldException Exceptions from clients passing invalid fields.
     */
    @Transactional
    public Page<T> queryDatabase(
        final Map<String, String[]> parameters,
        final CriteriaBuilder criteriaBuilder,
        final CriteriaQuery<?> criteriaQuery,
        final Root<?> root,
        final PageRequest pageRequest,
        final Class<?> clazz) throws NoSuchFieldException {

        var predicates = this.getPredicates(
            parameters,
            criteriaBuilder,
            criteriaQuery,
            root,
            clazz);

        // Create a "compound predicate" - a conjunction - from all the filtered parameters.
        var conjunction = criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));
        criteriaQuery.where(conjunction);

        var listSize = pageRequest.getPageSize();
        if (listSize <= 0) {
            listSize = 1;
        }

        // Build our query results from our requested page.
        var query = this.entityManager.createQuery(criteriaQuery)
            .setFirstResult(pageRequest.getPageNumber() * pageRequest.getPageSize())
            .setHint("org.hibernate.cacheable", true)
            .setMaxResults(listSize);
        var records = query.getResultList();

        var count = this.getCount(parameters, clazz);
        var paged = new GenericPageImplementation<>(records, pageRequest, count);
        return (GenericPageImplementation<T>) paged;
    }

    /**
     * Retrieve the number of records matching a set of parameters.
     *
     * @param parameters The parameters reprsenting predicates.
     * @param clazz      The class we're building a count for.
     * @return The number of matching records.
     * @throws NoSuchFieldException Exceptions from clients passing invalid fields.
     */
    public Long getCount(
        final Map<String, String[]> parameters,
        final Class<?> clazz)
        throws NoSuchFieldException {

        var criteriaBuilder = this.entityManager.getCriteriaBuilder();

        // Create a count query that matches our filter parameters so that we know how many
        // records match our predicates.
        var countQuery = criteriaBuilder.createQuery(Long.class);
        var countRoot = (Root<?>) countQuery.from(clazz);

        var predicates = this.getPredicates(
            parameters,
            criteriaBuilder,
            countQuery,
            countRoot,
            clazz);

        countQuery
            .select(criteriaBuilder.count(countRoot))
            .where(criteriaBuilder
                .and(predicates.toArray(new Predicate[predicates.size()])));

        var count = this.entityManager.createQuery(countQuery).getSingleResult();
        return count;
    }

    /**
     * Build a list of predicates provided the parameters.
     *
     * @param parameters      The parameters to use towards building predicates.
     * @param criteriaBuilder The criteria builder.
     * @param criteriaQuery   The JPA query.
     * @param root            Our root-most class representing our top-level table to query.
     * @param clazz           The class we're building a query against.
     * @return A list of predicates.
     * @throws NoSuchFieldException Exceptions from clients passing invalid fields.
     */
    public List<Predicate> getPredicates(
        final Map<String, String[]> parameters,
        final CriteriaBuilder criteriaBuilder,
        final CriteriaQuery criteriaQuery,
        final Root<?> root,
        final Class<?> clazz) throws NoSuchFieldException {

        var predicates = new ArrayList<Predicate>();
        for (var keychain : parameters.keySet()) {

            var value = parameters.get(keychain)[0];

            // Split the filter value to determine if it represents a nested query...
            var split = Arrays.asList(keychain.split("\\."));

            if (split.size() > 1) {

                // ...if it is a nested query, process it accordingly...
                predicates.add(this.getPredicatesForNestedParameters(
                    criteriaBuilder,
                    criteriaQuery,
                    root,
                    keychain,
                    split,
                    value,
                    clazz));

                // Otherwise, process it is as a single query.
            } else {
                predicates.add(this.getPredicateForParameter(
                    criteriaBuilder,
                    split.get(0),
                    value,
                    root,
                    clazz));
            }
        }

        return predicates;
    }

    /**
     * Build a predicate for the provided field and value of the field.
     *
     * @param criteriaBuilder The criteria builder.
     * @param fieldKey        The Reflection field.
     * @param value           The value of our field.
     * @param root            The root-most class representing the table we're querying.
     * @param clazz           The class we're querying.
     * @return A predicate.
     * @throws NoSuchFieldException Exceptions from clients passing invalid fields.
     */
    private Predicate getPredicateForParameter(
        final CriteriaBuilder criteriaBuilder,
        final String fieldKey,
        final String value,
        final Root<?> root,
        final Class<?> clazz) throws NoSuchFieldException {

        Predicate predicate = null;

        var fields = FieldUtils.getAllFieldsList(clazz);
        var keyWord = this.getStringWithoutOperatorSymbols(fieldKey);
        var match = fields.stream().filter(x -> x.getName().equalsIgnoreCase(keyWord)).findFirst();
        if (match.isPresent()) {

            var field = match.get();
            var filterParam = this.getQueryFilterParameter(fieldKey, value, keyWord);

            predicate = this.getPredicateForParameterHelper(
                criteriaBuilder,
                field,
                root,
                filterParam);
        } else {
            throw new NoSuchFieldException("ERROR: Could not match filter param "
                + keyWord
                + " of keychain "
                + fieldKey
                + " to any field in "
                + clazz);
        }
        return predicate;
    }

    /**
     * Helper method to get the parameter equivalent for a Query Filter.
     *
     * @param fieldKey The field to build a parameter for.
     * @param value    The value to build a parameter for.
     * @param keyWord  The actual value for our eventual predicate.
     * @return A filter parameter.
     */
    private QueryFilterParameter getQueryFilterParameter(
        final String fieldKey,
        final String value,
        final String keyWord) {

        var filterParam = new QueryFilterParameter();
        filterParam.setValue(value);
        filterParam.setKey(keyWord);

        // Attempt to derive the operand of our query.
        if (fieldKey.substring(fieldKey.length() - 1).equalsIgnoreCase("<")) {
            filterParam.setOperator(QueryOperatorType.LESS_THAN);
        } else if (fieldKey.substring(fieldKey.length() - 1).equalsIgnoreCase(">")) {
            filterParam.setOperator(QueryOperatorType.GREATER_THAN);
        } else if (fieldKey.substring(fieldKey.length() - 1).equalsIgnoreCase("*")) {
            filterParam.setOperator(QueryOperatorType.LIKE);
        } else {
            filterParam.setOperator(QueryOperatorType.EQUAL);
        }
        return filterParam;
    }

    /**
     * Given a period-delimited keychain, attempt to generate a joined query.
     *
     * @param criteriaBuilder The criteria builder.
     * @param criteriaQuery   The criteria query.
     * @param root            The root query object.
     * @param keychain        The period-delimited keychain representing a nested query request.
     * @param split           The keychain split into a list.
     * @param queryValue      The value we're attempting to build a query for.
     * @throws NoSuchFieldException Exceptions from invalid field requests.
     */
    private Predicate getPredicatesForNestedParameters(
        final CriteriaBuilder criteriaBuilder,
        final CriteriaQuery<?> criteriaQuery,
        final Root<?> root,
        final String keychain,
        final List<String> split,
        final String queryValue,
        final Class<?> rootClass) throws NoSuchFieldException {

        // Determine the final class and field type for the last element of the keychain
        var currentClass = rootClass;
        Field finalField = null;

        for (int i = 0; i < split.size(); i++) {
            String fieldName = getStringWithoutOperatorSymbols(split.get(i));
            Optional<Field> fieldOpt = FieldUtils.getAllFieldsList(currentClass).stream()
                .filter(f -> f.getName().equalsIgnoreCase(fieldName))
                .findFirst();

            if (fieldOpt.isEmpty()) {
                logger.debug("Field '{}' not found in '{}', checking subclasses...",
                    fieldName, currentClass.getSimpleName());

                currentClass = classDeriver.tryGetPredicateClass(currentClass, fieldName);

                fieldOpt = FieldUtils.getAllFieldsList(currentClass).stream()
                    .filter(f -> f.getName().equalsIgnoreCase(fieldName))
                    .findFirst();

                if (fieldOpt.isEmpty()) {
                    throw new NoSuchFieldException("Field '" + fieldName + "' not found in "
                        + currentClass.getSimpleName() + " or its subclasses.");
                }
            }

            finalField = fieldOpt.get();

            // If it's a collection, resolve the generic type for the next iteration
            if (Collection.class.isAssignableFrom(finalField.getType())) {
                var collectionType = (ParameterizedType) finalField.getGenericType();
                currentClass = (Class<?>) collectionType.getActualTypeArguments()[0];
            } else {
                currentClass = finalField.getType();
            }
        }

        // Build the subquery using correlated joins
        var subQuery = criteriaQuery.subquery(currentClass);

        Root<?> subRoot = null;
        if (this.entityDeriver.isEntityClass(currentClass)) {
            subRoot = subQuery.from(currentClass);
        } else {
            subRoot = subQuery.correlate(root);
        }

        var join = subRoot.join(getStringWithoutOperatorSymbols(split.get(0)));
        for (int i = 1; i < split.size() - 1; i++) {
            String fieldName = getStringWithoutOperatorSymbols(split.get(i));
            join = join.join(fieldName);
        }

        var finalFieldName = getStringWithoutOperatorSymbols(split.get(split.size() - 1));
        var joinedPath = join.get(finalFieldName);

        if (finalField == null) {
            throw new NoSuchFieldException("Final field '"
                + finalFieldName
                + "' not found in the keychain: "
                + keychain);
        }

        return getPredicateForNestedParameterHelper(
            criteriaBuilder,
            joinedPath,
            subQuery,
            keychain,
            finalField,
            queryValue);
    }


    /**
     * Private helper method to build a predicate from a filter parameter.
     *
     * @param criteriaBuilder The criteria builder.
     * @param field           The field we're dealing with.
     * @param root            The root query.
     * @param parameter       The parameter we're using to build a predicate from.
     * @return A predicate.
     */
    private Predicate getPredicateForParameterHelper(
        final CriteriaBuilder criteriaBuilder,
        final Field field,
        final Root<?> root,
        final QueryFilterParameter parameter) {

        Predicate predicate = null;
        // Attempt to derive the type from the
        // request using a combination of parsing and reflection.
        if (parameter.getValue().equalsIgnoreCase("null")) {
            predicate = criteriaBuilder.isNull(root.get(parameter.getKey()));
        } else if (parameter.getValue().equalsIgnoreCase("!null")) {
            predicate = criteriaBuilder.isNotNull(root.get(parameter.getKey()));
        } else {
            if (field.getType().equals(OffsetDateTime.class)) {
                predicate = this.nonNestedPredicateBuilder.getPredicateHelperDate(
                    criteriaBuilder,
                    root,
                    parameter);
            } else if (field.getType().equals(String.class)) {
                predicate = this.nonNestedPredicateBuilder.getPredicateHelperString(
                    criteriaBuilder,
                    root,
                    parameter);
            } else if (field.getType().equals(Boolean.class)) {
                predicate = this.nonNestedPredicateBuilder.getPredicateHelperBoolean(
                    criteriaBuilder,
                    root,
                    parameter);
            } else if (field.getType().equals(Integer.class)) {
                predicate = this.nonNestedPredicateBuilder.getPredicateHelperInteger(
                    criteriaBuilder,
                    root,
                    parameter);
            } else if (field.getType().equals(Float.class)) {
                predicate = this.nonNestedPredicateBuilder.getPredicateHelperFloat(
                    criteriaBuilder,
                    root,
                    parameter);
            } else if (field.getType().equals(Double.class)) {
                predicate = this.nonNestedPredicateBuilder.getPredicateHelperDouble(
                    criteriaBuilder,
                    root,
                    parameter);
            } else if (field.getType().isEnum()) {
                predicate = this.nonNestedPredicateBuilder.getPredicateHelperEnum(
                    criteriaBuilder,
                    root,
                    field,
                    parameter);
            }
        }
        return predicate;
    }

    /**
     * Helper method to help appropriately derive our parameter type.
     *
     * @param criteriaBuilder The criteria builder.
     * @param joined          Our joined tables.
     * @param subQuery        The subquery after joining.
     * @param keychain        Our keychain denoting our desired model to query.
     * @param subField        The specific field we're querying for.
     * @param queryValue      The value we're querying for.
     * @return A predicate.
     */
    private Predicate getPredicateForNestedParameterHelper(
        final CriteriaBuilder criteriaBuilder,
        final Path joined,
        final Subquery subQuery,
        final String keychain,
        final Field subField,
        final String queryValue) {

        Predicate predicate = null;
        if (subField.getType().equals(OffsetDateTime.class)) {
            predicate = this.nestedPredicateBuilder.getNestedQueryPredicate(
                keychain,
                criteriaBuilder,
                joined,
                subQuery,
                queryValue,
                OffsetDateTime.class);
        } else if (subField.getType().equals(Integer.class)) {
            predicate = this.nestedPredicateBuilder.getNestedQueryPredicate(
                keychain,
                criteriaBuilder,
                joined,
                subQuery,
                queryValue,
                Integer.class);
        } else if (subField.getType().equals(String.class)) {
            predicate = this.nestedPredicateBuilder.getNestedQueryPredicate(
                keychain,
                criteriaBuilder,
                joined,
                subQuery,
                queryValue,
                String.class);
        } else if (subField.getType().equals(Double.class)) {
            predicate = this.nestedPredicateBuilder.getNestedQueryPredicate(
                keychain,
                criteriaBuilder,
                joined,
                subQuery,
                queryValue,
                Double.class);
        } else if (subField.getType().equals(Float.class)) {
            predicate = this.nestedPredicateBuilder.getNestedQueryPredicate(
                keychain,
                criteriaBuilder,
                joined,
                subQuery,
                queryValue,
                Float.class);
        } else if (subField.getType().equals(Boolean.class)) {
            predicate = this.nestedPredicateBuilder.getNestedQueryPredicate(
                keychain,
                criteriaBuilder,
                joined,
                subQuery,
                queryValue,
                Boolean.class);
        } else if (subField.getType().isEnum()) {
            predicate = this.nestedPredicateBuilder.getNestedQueryPredicateEnum(
                criteriaBuilder,
                joined,
                subQuery,
                subField,
                queryValue);
        }
        return predicate;
    }

    /**
     * Given a string, build and return the key without any operator symbols.
     *
     * @param key The key we're replacing strings from.
     * @return A string without the symbols.
     */
    private String getStringWithoutOperatorSymbols(final String key) {
        return key.replaceAll("[<>*]", "");
    }

    /**
     * Attempt to sort the order of our database query.
     *
     * @param sortByFields    The fields to sort by.
     * @param sortDescending  Whether or not to sort by descending.
     * @param criteriaBuilder The criteria builder.
     * @param root            The root-most class representing our root-most table.
     * @param criteriaQuery   The JPA query.
     */
    private void trySetOrderBy(
        final List<String> sortByFields,
        final Boolean sortDescending,
        final CriteriaBuilder criteriaBuilder,
        final Root<?> root,
        CriteriaQuery criteriaQuery) {

        if (Objects.nonNull(sortByFields)) {
            for (var sortByField : sortByFields) {
                Order order = null;
                if (sortDescending) {
                    order = criteriaBuilder.desc(root.get(sortByField));
                } else {
                    order = criteriaBuilder.asc(root.get(sortByField));
                }
                criteriaQuery.orderBy(order);
            }
        }
    }



    /**
     * Get a page request.
     *
     * @param sortByFields   The fields to sort by.
     * @param sortDescending Whether or not to sort descending.
     * @param page           The page to query for.
     * @param size           The page size.
     * @return A Page request.
     */
    private PageRequest getPageRequest(
        final List<String> sortByFields,
        final Boolean sortDescending,
        final int page,
        final int size) {

        var orders = new ArrayList<Sort.Order>();
        if (Objects.nonNull(sortByFields)) {
            for (var sortByField : sortByFields) {
                var direction = Sort.Direction.ASC;
                if (Objects.nonNull(sortDescending) && sortDescending) {
                    direction = Sort.Direction.DESC;
                }
                var order = new Sort.Order(direction, sortByField);
                orders.add(order);
            }
        }
        var sort = Sort.by(orders);
        var pageRequest = PageRequest.of(page, size, sort);
        return pageRequest;
    }
}
