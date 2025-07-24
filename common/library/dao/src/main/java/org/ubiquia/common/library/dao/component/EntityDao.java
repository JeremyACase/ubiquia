package org.ubiquia.common.library.dao.component;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.ubiquia.common.model.ubiquia.dao.QueryFilter;

/**
 * Abandon all hope, ye who enter here.
 * This is a Data Access Object (DAO) component dedicated to building
 * dynamic queries based on filter parameters for
 * templated classes. It leverages JPA's CriteriaQuery methods to generate dynamic database
 * queries. This code also utilizes reflection to determine types at runtime.
 */
@Component
public class EntityDao<T> {

    @Autowired
    private ParameterDao<T> parameterDataAccessObject;

    @Autowired
    private FilterDao<T> filterDataAccessObject;

    /**
     * Retrieve the count of records provided the parameters.
     *
     * @param parameters The parameters representing predicates.
     * @param clazz      The class we're getting a count for.
     * @return A long representing a count.
     * @throws NoSuchFieldException Exceptions from invalid fields.
     */
    public Long getCount(
        final Map<String, String[]> parameters,
        final Class<T> clazz) throws NoSuchFieldException {
        var count = this.parameterDataAccessObject.getCount(parameters, clazz);
        return count;
    }

    /**
     * Retrieve the count of entities that match a provided filter and subsequent predicates.
     *
     * @param queryFilter The filter to count entities for.
     * @param clazz       The class of the objects to get counts for.
     * @return A count representing the matching objects.
     * @throws NoSuchFieldException Exception from invalid fields.
     */
    public Long getCount(
        final QueryFilter queryFilter,
        final Class<T> clazz) throws NoSuchFieldException {
        var count = this.filterDataAccessObject.getCount(queryFilter, clazz);
        return count;
    }

    /**
     * Given a filter, build and return paginated results from the database.
     *
     * @param queryFilter The filter we're using.
     * @param page        The page we're querying for.
     * @param size        The size of pages we're querying for.
     * @param clazz       The class we're creating a DAO for.
     * @return A page of results per the filter.
     * @throws NoSuchFieldException Exceptions thrown when non-existent fields are passed.
     */
    @Transactional
    public Page<T> getPage(
        final QueryFilter queryFilter,
        final Integer page,
        final Integer size,
        final Class<T> clazz) throws NoSuchFieldException {

        var pageResponse = this.filterDataAccessObject.getPage(
            queryFilter,
            page,
            size,
            clazz);

        return pageResponse;
    }

    /**
     * Get a page of data provided a set of parameters.
     *
     * @param parameters     The query parameters received.
     * @param page           The page number to retrieve.
     * @param size           The size of the page.
     * @param sortDescending Whether or not to sort descending.
     * @param sortByFields   Fields to sort by.
     * @param clazz          The class we're getting a page of data for.
     * @return The Page of data.
     * @throws NoSuchFieldException Exceptions from invalid fields.
     */
    @Transactional
    public Page<T> getPage(
        final Map<String, String[]> parameters,
        final Integer page,
        final Integer size,
        final Boolean sortDescending,
        final List<String> sortByFields,
        final Class<T> clazz) throws NoSuchFieldException {

        var pageResponse = this.parameterDataAccessObject.getPage(
            parameters,
            page,
            size,
            sortDescending,
            sortByFields,
            clazz);

        return pageResponse;
    }

    /**
     * Get a page of data provided a set of parameters.
     *
     * @param parameters        The query parameters received.
     * @param page              The page number to retrieve.
     * @param size              The size of the page.
     * @param sortDescending    Whether or not to sort descending.
     * @param sortByFields      Fields to sort by.
     * @param multiselectFields Fields to select.
     * @param clazz             The class we're getting a page of data for.
     * @return The Page of data.
     * @throws NoSuchFieldException Exceptions from invalid fields.
     */
    @Transactional
    public Page<Object[]> getPageMultiselect(
        final Map<String, String[]> parameters,
        final Integer page,
        final Integer size,
        final Boolean sortDescending,
        final List<String> sortByFields,
        final List<String> multiselectFields,
        final Class<T> clazz) throws NoSuchFieldException {

        var pageResponse = this.parameterDataAccessObject.getPageMultiSelect(
            parameters,
            page,
            size,
            sortDescending,
            sortByFields,
            multiselectFields,
            clazz);

        return pageResponse;
    }
}
