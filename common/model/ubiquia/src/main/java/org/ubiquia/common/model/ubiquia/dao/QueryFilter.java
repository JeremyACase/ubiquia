package org.ubiquia.common.model.ubiquia.dao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;


/**
 * A filter model used to query the back-end for a given parameterized set of data.
 */
@Validated
public class QueryFilter {
    private SortType sort = null;

    @Valid
    private List<String> sortBy = null;

    @Valid
    private List<QueryFilterParameter> parameters = new ArrayList<>();

    private Integer page = 0;

    private Integer pageSize = 1;

    /**
     * Get sort.
     *
     * @return sort
     **/
    @Valid
    public SortType getSort() {
        return sort;
    }

    public void setSort(SortType sort) {
        this.sort = sort;
    }

    /**
     * Get sortBy.
     *
     * @return sortBy
     **/
    public List<String> getSortBy() {
        return sortBy;
    }

    public void setSortBy(List<String> sortBy) {
        this.sortBy = sortBy;
    }

    /**
     * Get parameters.
     *
     * @return parameters
     **/
    @Valid
    public List<QueryFilterParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<QueryFilterParameter> parameters) {
        this.parameters = parameters;
    }

    /**
     * Get page.
     *
     * @return page
     **/
    @NotNull
    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    /**
     * Get pageSize.
     *
     * @return pageSize
     **/
    @NotNull
    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
