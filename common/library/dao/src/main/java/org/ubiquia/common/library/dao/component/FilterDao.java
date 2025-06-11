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
import org.ubiquia.common.library.dao.service.builder.NestedPredicateBuilder;
import org.ubiquia.common.library.dao.service.builder.NonNestedPredicateBuilder;
import org.ubiquia.common.library.dao.service.logic.ClassDeriver;
import org.ubiquia.common.library.dao.service.logic.EntityDeriver;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.dao.QueryFilter;
import org.ubiquia.common.model.ubiquia.dao.QueryFilterParameter;
import org.ubiquia.common.model.ubiquia.dao.SortType;

/**
 * This is a Data Access Object (DAO) that can be used to query data from a relational database
 * for a specific entity class provided a "Query Filter."
 *
 * @param <T> The entity type that the DAO is responsible for.
 */
@Component
public class FilterDao<T> {

    private static final Logger logger = LoggerFactory.getLogger(FilterDao.class);
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
     * Given a filter, build and return paginated results from the database.
     *
     * @param queryFilter The filter we're using.
     * @param page        The page we're querying for.
     * @param size        The size of pages we're querying for.
     * @param clazz       The class we're querying for.
     * @return A page of results per the filter.
     * @throws NoSuchFieldException Exceptions thrown when non-existent fields are passed.
     */
    @Transactional
    public Page<T> getPage(
        final QueryFilter queryFilter,
        final int page,
        final int size,
        final Class<T> clazz) throws NoSuchFieldException {

        var predicateClass = this.classDeriver.tryGetPredicateClass(clazz, queryFilter);

        var criteriaBuilder = this.entityManager.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(predicateClass);
        var root = criteriaQuery.from(predicateClass);

        this.trySetOrderBy(queryFilter, criteriaBuilder, root, criteriaQuery);
        var pageRequest = this.getPageRequest(queryFilter, page, size);

        var pageResponse = this.queryDatabase(
            queryFilter,
            criteriaBuilder,
            criteriaQuery,
            root,
            pageRequest,
            predicateClass);

        return pageResponse;
    }

    /**
     * Provided a filter, return the matching number of records.
     *
     * @param queryFilter The filter representing predicates.
     * @param clazz       The class we're counting records for.
     * @return A count representing the matching number of records.
     * @throws NoSuchFieldException Exceptions from invalid fields.
     */
    public Long getCount(
        final QueryFilter queryFilter,
        final Class<?> clazz) throws NoSuchFieldException {

        var criteriaBuilder = this.entityManager.getCriteriaBuilder();

        // Create a count query that matches our filter parameters so that we know how many
        // records match our predicates.
        var countQuery = criteriaBuilder.createQuery(Long.class);
        var rootCount = countQuery.from(clazz);

        var predicates = this.getPredicates(
            queryFilter,
            criteriaBuilder,
            countQuery,
            rootCount,
            clazz);

        countQuery
            .select(criteriaBuilder.count(rootCount))
            .where(criteriaBuilder
                .and(predicates.toArray(new Predicate[predicates.size()])));

        var count = this.entityManager.createQuery(countQuery).getSingleResult();
        return count;
    }


    /**
     * Query the database provided the filters.
     *
     * @param queryFilter     The filter we're generating predicates for.
     * @param criteriaBuilder The Criteria Builder.
     * @param criteriaQuery   Our top-level JPA query.
     * @param root            The root-level table we're querying from.
     * @param pageRequest     The page configuration.
     * @param clazz           The class we're querying for.
     * @return A page of data.
     * @throws NoSuchFieldException Exceptions from passing in invalid fields.
     */
    @SuppressWarnings("unchecked")
    @Transactional
    private Page<T> queryDatabase(
        final QueryFilter queryFilter,
        final CriteriaBuilder criteriaBuilder,
        final CriteriaQuery<?> criteriaQuery,
        final Root<?> root,
        final PageRequest pageRequest,
        final Class<?> clazz) throws NoSuchFieldException {

        var predicateClass = this.classDeriver.tryGetPredicateClass(clazz, queryFilter);

        var predicates = this.getPredicates(
            queryFilter,
            criteriaBuilder,
            criteriaQuery,
            root,
            predicateClass);

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

        var count = this.getCount(queryFilter, clazz);
        var paged = new GenericPageImplementation<>(records, pageRequest, count);
        return (GenericPageImplementation<T>) paged;
    }

    /**
     * Given a filter and a class, build and return predicates
     * to use for querying records from the database.
     *
     * @param filter          The filter we're building predicates from.
     * @param criteriaBuilder The criteria builder.
     * @param root            The root query.
     * @param clazz           The class we're getting predicates for.
     * @return A list of predicates for our query.
     */
    private List<Predicate> getPredicates(
        final QueryFilter filter,
        final CriteriaBuilder criteriaBuilder,
        final CriteriaQuery<?> criteriaQuery,
        final Root<?> root,
        final Class<?> clazz) throws NoSuchFieldException {

        var predicates = new ArrayList<Predicate>();
        for (var filterParam : filter.getParameters()) {

            // Split the filter value to determine if it represents a nested query...
            var split = Arrays.asList(filterParam.getKey().split("\\."));

            // ...if it is a nested query, process it accordingly...
            if (split.size() > 1) {
                predicates.add(this.getPredicatesForNestedParameters(
                    criteriaBuilder,
                    criteriaQuery,
                    root,
                    filterParam,
                    split,
                    clazz));

                // Otherwise, process it is as a single query.
            } else {
                predicates.add(this.getPredicateForParameter(
                    criteriaBuilder,
                    filterParam,
                    root,
                    clazz));
            }
        }

        return predicates;
    }

    /**
     * Given a nested query, validate and join the query to formulate filter parameters.
     *
     * @param criteriaBuilder Our Criteria Builder.
     * @param root            The root value of our JPA query.
     * @param filterParam     The filter parameter to get a predicate for.
     * @param clazz           The class we're building predicates for.
     * @return A predicate.
     */
    private Predicate getPredicateForParameter(
        final CriteriaBuilder criteriaBuilder,
        final QueryFilterParameter filterParam,
        final Root<?> root,
        final Class<?> clazz) {

        Predicate predicate = null;

        var fields = FieldUtils.getAllFieldsList(clazz);
        var match = fields.stream().filter(x ->
                x.getName().equalsIgnoreCase(filterParam.getKey()))
            .findFirst();
        if (match.isPresent()) {

            var field = match.get();
            predicate = this.getPredicateForParameterHelper(
                criteriaBuilder,
                field,
                root,
                filterParam);
        }
        return predicate;
    }

    /**
     * Given a period-delimited keychain, attempt to generate a joined query.
     *
     * @param criteriaBuilder The criteria builder.
     * @param criteriaQuery   The criteria query.
     * @param root            The root query object.
     * @param split           The keychain split into a list.
     * @param clazz           The class we're building predicates for.
     * @throws NoSuchFieldException Exceptions from invalid field requests.
     */
    private Predicate getPredicatesForNestedParameters(
        final CriteriaBuilder criteriaBuilder,
        final CriteriaQuery<?> criteriaQuery,
        final Root<?> root,
        final QueryFilterParameter filterParameter,
        final List<String> split,
        final Class<?> clazz) throws NoSuchFieldException {

        // Resolve the final class and final field from the nested keychain...
        var currentClass = clazz;
        Field finalField = null;

        for (var segment : split) {
            var cleanSegment = this.getStringWithoutOperatorSymbols(segment);

            // Find the field in the current class (or subclass)
            var field = findFieldIgnoreCase(currentClass, cleanSegment);
            if (Objects.isNull(field)) {
                logger.debug("Field '{}' not found in '{}', checking subclasses...",
                    cleanSegment,
                    currentClass.getSimpleName());
                currentClass = this.classDeriver.tryGetPredicateClass(currentClass, cleanSegment);
                field = findFieldIgnoreCase(currentClass, cleanSegment);

                if (Objects.isNull(field)) {
                    throw new NoSuchFieldException("Field '"
                        + cleanSegment
                        + "' not found in "
                        + currentClass.getSimpleName()
                        + " or its subclasses.");
                }
            }

            finalField = field;

            // If this field is a collection, resolve the generic type for the next iteration
            if (Collection.class.isAssignableFrom(field.getType())) {
                var collectionType = (ParameterizedType) field.getGenericType();
                currentClass = (Class<?>) collectionType.getActualTypeArguments()[0];
            } else {
                currentClass = field.getType();
            }
        }

        // ...build the subquery with correlated joins...
        var subQuery = criteriaQuery.subquery(currentClass);
        Root<?> subRoot = null;
        if (this.entityDeriver.isEntityClass(currentClass)) {
            subRoot = subQuery.from(currentClass);
        } else {
            subRoot = subQuery.correlate(root);
        }

        var join = subRoot.join(getStringWithoutOperatorSymbols(split.get(0)));
        for (int i = 1; i < split.size() - 1; i++) {
            join = join.join(getStringWithoutOperatorSymbols(split.get(i)));
        }

        var finalFieldName = getStringWithoutOperatorSymbols(split.get(split.size() - 1));
        var joinedPath = join.get(finalFieldName);

        // ...validate finalField presence...
        if (finalField == null) {
            throw new NoSuchFieldException("Final field '"
                + finalFieldName
                + "' not found in keychain: "
                + String.join(".", split));
        }

        // ...build and return the nested predicate...
        return getPredicateForNestedParameterHelper(
            filterParameter,
            criteriaBuilder,
            joinedPath,
            subQuery,
            finalField);
    }

    /**
     * Helper: Find a field in a class by name (case-insensitive).
     */
    private Field findFieldIgnoreCase(Class<?> clazz, String fieldName) {
        return FieldUtils.getAllFieldsList(clazz).stream()
            .filter(f -> f.getName().equalsIgnoreCase(fieldName))
            .findFirst()
            .orElse(null);
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

        if (parameter.getValue().equalsIgnoreCase("null")) {
            predicate = criteriaBuilder.isNull(root.get(parameter.getKey()));
        } else if (parameter.getValue().equalsIgnoreCase("!null")) {
            predicate = criteriaBuilder.isNotNull(root.get(parameter.getKey()));
        } else {

            if (field.getType().equals(OffsetDateTime.class)) {
                predicate = this.nonNestedPredicateBuilder.getPredicateHelperDate(
                    criteriaBuilder,
                    root,
                    parameter
                );
            } else if (field.getType().equals(Float.class)) {
                predicate = this.nonNestedPredicateBuilder.getPredicateHelperFloat(
                    criteriaBuilder,
                    root,
                    parameter
                );
            } else if (field.getType().equals(Double.class)) {
                predicate = this.nonNestedPredicateBuilder.getPredicateHelperDouble(
                    criteriaBuilder,
                    root,
                    parameter
                );
            } else if (field.getType().equals(Integer.class)) {
                predicate = this.nonNestedPredicateBuilder.getPredicateHelperInteger(
                    criteriaBuilder,
                    root,
                    parameter
                );
            } else if (field.getType().equals(String.class)) {
                predicate = this.nonNestedPredicateBuilder.getPredicateHelperString(
                    criteriaBuilder,
                    root,
                    parameter
                );
            } else if (field.getType().isEnum()) {
                predicate = this.nonNestedPredicateBuilder.getPredicateHelperEnum(
                    criteriaBuilder,
                    root,
                    field,
                    parameter
                );
            } else if (field.getType().equals(Boolean.class)) {
                predicate = this.nonNestedPredicateBuilder.getPredicateHelperBoolean(
                    criteriaBuilder,
                    root,
                    parameter
                );
            }
        }
        return predicate;
    }

    /**
     * Helper method to return properly-typed predicates.
     *
     * @param filterParameter The parameter we're using to get nested predicates from.
     * @param criteriaBuilder JPA stuff.
     * @param joined          Our joined SQL query (basically.)
     * @param subQuery        Our current SQL statement to add to our top-level query.
     * @param subField        The field we're building a predicate for.
     * @return The predicate.
     */
    private Predicate getPredicateForNestedParameterHelper(
        final QueryFilterParameter filterParameter,
        final CriteriaBuilder criteriaBuilder,
        final Path joined,
        final Subquery subQuery,
        final Field subField) {

        Predicate predicate = null;
        if (subField.getType().isEnum()) {

            predicate = this.nestedPredicateBuilder.getNestedQueryPredicateEnum(
                criteriaBuilder,
                joined,
                subQuery,
                subField,
                filterParameter.getValue());
        } else {
            predicate = this.nestedPredicateBuilder.getNestedQueryPredicate(
                filterParameter,
                criteriaBuilder,
                joined,
                subQuery);
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
     * Try to set our order by criteria.
     *
     * @param queryFilter     The filter to use to build our order by.
     * @param criteriaBuilder The JPA criteria builder.
     * @param root            The root-level table/class we're querying for.
     * @param criteriaQuery   The JPA query.
     */
    private void trySetOrderBy(
        final QueryFilter queryFilter,
        final CriteriaBuilder criteriaBuilder,
        final Root<?> root,
        CriteriaQuery criteriaQuery) {

        if (Objects.nonNull(queryFilter.getSortBy())) {
            for (var sortByField : queryFilter.getSortBy()) {
                Order order = null;
                if (SortType.DESCENDING.equals(queryFilter.getSort())) {
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
     * @param queryFilter The query filter we're building a page for.
     * @param page        The page number.
     * @param size        The page size.
     * @return A page request.
     */
    private PageRequest getPageRequest(
        final QueryFilter queryFilter,
        final int page,
        final int size) {

        var orders = new ArrayList<Sort.Order>();
        if (Objects.nonNull(queryFilter.getSortBy())) {
            for (var sortByField : queryFilter.getSortBy()) {
                var direction = Sort.Direction.ASC;
                if (Objects.nonNull(queryFilter.getSort())
                    && SortType.DESCENDING.equals(queryFilter.getSort())) {
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
