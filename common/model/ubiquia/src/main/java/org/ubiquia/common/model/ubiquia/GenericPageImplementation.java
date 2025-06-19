package org.ubiquia.common.model.ubiquia;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Generic page implementation used for serializing data in.
 *
 * @param <T> The type for our Page Request.
 */
public class GenericPageImplementation<T> extends PageImpl<T> {

    @Autowired
    private ObjectMapper objectMapper;

    private long totalElements;

    /**
     * Constructor.
     *
     * @param content          The list of content.
     * @param number           Number.
     * @param size             Size.
     * @param totalElements    Total elements from the database.
     * @param pageable         Pageable metadata.
     * @param last             Whether or not this is the last page.
     * @param totalPages       The total number of pages.
     * @param sort             Sorting metadata.
     * @param first            Whether or not this is the first page.
     * @param numberOfElements The number of elements in this page.
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public GenericPageImplementation(
        @JsonProperty("content") List<T> content,
        @JsonProperty("number") int number,
        @JsonProperty("size") int size,
        @JsonProperty("totalElements") Long totalElements,
        @JsonProperty("pageable") JsonNode pageable,
        @JsonProperty("last") boolean last,
        @JsonProperty("totalPages") int totalPages,
        @JsonProperty("sort") JsonNode sort,
        @JsonProperty("first") boolean first,
        @JsonProperty("numberOfElements") int numberOfElements,
        @JsonProperty("empty") boolean empty) {

        super(content, PageRequest.of(number, size), totalElements);
        this.totalElements = totalElements;
    }

    /**
     * Constructor.
     *
     * @param content       The list of content.
     * @param totalElements Total elements from the database.
     * @param pageable      Pageable metadata.
     */
    public GenericPageImplementation(List<T> content, Pageable pageable, long totalElements) {
        super(content, pageable, totalElements);
        this.totalElements = totalElements;
    }

    /**
     * Constructor.
     *
     * @param content The list of content.
     */
    public GenericPageImplementation(List<T> content) {
        super(content);
    }

    /**
     * Constructor.
     */
    public GenericPageImplementation() {
        super(new ArrayList());
    }
}
