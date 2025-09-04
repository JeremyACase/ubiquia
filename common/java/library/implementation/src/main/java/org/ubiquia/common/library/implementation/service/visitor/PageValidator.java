package org.ubiquia.common.library.implementation.service.visitor;


import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PageValidator {

    @Value("${ubiquia.page.max-size:100}")
    protected Integer maxPageSize;

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
