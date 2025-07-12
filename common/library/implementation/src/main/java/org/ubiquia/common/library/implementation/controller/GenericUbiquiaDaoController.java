package org.ubiquia.common.library.implementation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.library.dao.interfaces.InterfaceUbiquiaDaoController;
import org.ubiquia.common.library.implementation.service.builder.IngressResponseBuilder;
import org.ubiquia.common.library.implementation.service.visitor.PageValidator;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.dto.AbstractModel;
import org.ubiquia.common.model.ubiquia.entity.AbstractModelEntity;

/**
 * An abstract RESTful controller to be inherited by other controllers. As the base class
 * controller, it defines a handful of useful variables and features for derived classes.
 */
public abstract class GenericUbiquiaDaoController<
    T extends AbstractModelEntity,
    D extends AbstractModel>
    implements InterfaceLogger,
    InterfaceUbiquiaDaoController<T, D> {

    protected final Pattern camelcaseRegex;

    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected IngressResponseBuilder ingressResponseBuilder;
    protected Class<T> persistedEntityClass;
    protected Class<D> persistedDTOClass;
    @Autowired
    private PageValidator pageValidator;

    /**
     * Abandon all hope, ye who enter here.
     */
    public GenericUbiquiaDaoController() {

        this.getLogger().info("Initializing...");

        this.camelcaseRegex = Pattern.compile("-([a-z])");

        // Cache our persistent class in derived classes.
        this.persistedEntityClass = (Class<T>) ((ParameterizedType) this.getClass()
            .getGenericSuperclass()).getActualTypeArguments()[0];

        this.persistedDTOClass = (Class<D>) ((ParameterizedType) this.getClass()
            .getGenericSuperclass()).getActualTypeArguments()[1];
        this.getLogger().info("...initialized...");
    }

    /**
     * GET from this controller to query data from the database.
     *
     * @param page               The page number to retrieve.
     * @param size               The page size to retrieve.
     * @param sortDescending     Whether or not to sort the results descending or ascending.
     * @param sortByFields       The fields to sort by.
     * @param httpServletRequest The servlet request to use to parse the query.
     * @return A paginated response with records from the database.
     * @throws NoSuchFieldException    Exceptions from requesting fields that don't exist.
     * @throws JsonProcessingException Exceptions from processing fields retrieved from database.
     */
    @GetMapping("/query/params")
    public GenericPageImplementation<D> queryWithParams(
        @RequestParam("page") final Integer page,
        @RequestParam("size") final Integer size,
        @RequestParam(value = "sort-descending", required = false, defaultValue = "true") final Boolean sortDescending,
        @RequestParam(value = "sort-by-fields", required = false, defaultValue = "") final List<String> sortByFields,
        HttpServletRequest httpServletRequest)
        throws NoSuchFieldException,
        JsonProcessingException {

        var records = this.query(
            page,
            size,
            sortDescending,
            sortByFields,
            httpServletRequest);
        return records;
    }

    /**
     * A helper method to convert database records to DTO objects.
     *
     * @param records The records to map to DTO's.
     * @param mapper  The service that can map from entities to DTO's.
     * @return A page of converted DTO's.
     * @throws JsonProcessingException Exceptions from parsing database records.
     */
    protected GenericPageImplementation<D> convertPageHelper(
        final Page<T> records,
        final InterfaceEntityToDtoMapper<T, D> mapper)
        throws JsonProcessingException {

        GenericPageImplementation<D> convertedPage;
        var converted = mapper.map(records.getContent());
        convertedPage = new GenericPageImplementation<>(
            converted,
            records.getNumber(),
            records.getSize(),
            records.getTotalElements(),
            this.objectMapper.valueToTree(records.getPageable()),
            records.isLast(),
            records.getTotalPages(),
            this.objectMapper.valueToTree(records.getSort()),
            records.isFirst(),
            records.getNumberOfElements(),
            records.getSort().isEmpty());

        return convertedPage;
    }

    /**
     * A method that can query data from the database and convert the data to DTO's.
     *
     * @param page               The page number to retrieve.
     * @param size               The page size to retrieve.
     * @param sortDescending     Whether or not to sort the results descending or ascending.
     * @param sortByFields       The fields to sort by.
     * @param httpServletRequest The servlet request to use to parse the query.
     * @return A paginated response with records from the database.
     * @throws NoSuchFieldException    Exceptions from requesting fields that don't exist.
     * @throws JsonProcessingException Exceptions from processing fields retrieved from database.
     */
    protected GenericPageImplementation<D> query(
        Integer page,
        Integer size,
        Boolean sortDescending,
        List<String> sortByFields,
        HttpServletRequest httpServletRequest)
        throws NoSuchFieldException, JsonProcessingException {

        this.pageValidator.validatePageAndSize(page, size);
        var map = this.getMapFromServletRequest(httpServletRequest);
        this.getLogger().info("Received a query request by params: {}", map);

        var records = this.getDataAccessObject().getPage(
            map,
            page,
            size,
            sortDescending,
            sortByFields,
            this.persistedEntityClass);

        var egress = this.convertPageHelper(records, this.getDataTransferObjectMapper());
        return egress;
    }

    /**
     * Helper method to parse a servlet request into predicates.
     *
     * @param request The servlet request to parse.
     * @return A map of parameters to build predicates from.
     */
    private HashMap<String, String[]> getMapFromServletRequest(HttpServletRequest request) {
        var map = new HashMap<String, String[]>();
        for (var key : request.getParameterMap().keySet()) {
            var camelCase = this.camelcaseRegex.matcher(key)
                .replaceAll(x -> x.group(1).toUpperCase());
            map.put(camelCase, request.getParameterMap().get(key));
        }
        this.removePageKeysHelper(map);
        return map;
    }

    /**
     * Remove any superfluous keys from our map of parameters to query for.
     *
     * @param map The map to remove keys from.
     */
    private void removePageKeysHelper(HashMap<String, String[]> map) {
        map.remove("page");
        map.remove("size");
        map.remove("sortDescending");
        map.remove("sortByFields");
        map.remove("multiselectFields");
        map.remove("ignoreTokens");
    }
}
