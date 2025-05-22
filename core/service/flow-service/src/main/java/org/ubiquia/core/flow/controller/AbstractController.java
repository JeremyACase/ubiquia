package org.ubiquia.core.flow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import java.util.regex.Pattern;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public abstract class AbstractController {

    protected final Pattern camelcaseRegex;

    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${ubiquia.page.max-size}")
    protected Integer maxPageSize;

    /**
     * Ye olde' constructor.
     */
    public AbstractController() {
        this.getLogger().debug("Initializing...");
        this.camelcaseRegex = Pattern.compile("-([a-z])");
        this.getLogger().debug("...initialized...");
    }

    public Logger getLogger() {
        throw new NotImplementedException("ERROR: getLogger not implemented in AController!");
    }

    /**
     * Get this controller's max page size.
     *
     * @return The max page size.
     */
    public Integer getMaxPageSize() {
        return this.maxPageSize;
    }

    /**
     * Method to validate that clients aren't requesting page sizes out of bounds.
     *
     * @param page The page to validate.
     * @param size The size of the page.
     */
    protected void validatePage(final Integer page, final Integer size) {

        if (Objects.isNull(page)) {
            throw new IllegalArgumentException("Page cannot be null!");
        }

        if (Objects.isNull(size)) {
            throw new IllegalArgumentException("Page size cannot be null!");
        }

        if (size > this.getMaxPageSize()) {
            throw new IllegalArgumentException("Page request size is larger than maximum limit of: "
                + this.getMaxPageSize());
        }
    }
}
