package org.ubiquia.common.library.implementation.service.visitor;


import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Service that validates pagination parameters against configured limits. */
@Service
public class PageValidator {

    @Value("${ubiquia.page.max-size:100}")
    protected Integer maxPageSize;

    /**
     * Validate that the page and size parameters are non-null and within the allowed limit.
     *
     * @param page The zero-based page number.
     * @param size The number of records per page.
     * @throws IllegalArgumentException If page or size is null, or size exceeds the maximum.
     */
    public void validatePageAndSize(final Integer page, final Integer size) {

        if (Objects.isNull(page)) {
            throw new IllegalArgumentException("Page cannot be null!");
        }

        if (Objects.isNull(size)) {
            throw new IllegalArgumentException("Page size cannot be null!");
        }

        if (size > this.maxPageSize) {
            throw new IllegalArgumentException("Page request size is larger than maximum limit of: "
                + this.maxPageSize);
        }
    }
}
