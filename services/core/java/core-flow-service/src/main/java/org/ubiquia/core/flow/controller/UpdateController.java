package org.ubiquia.core.flow.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.library.implementation.service.mapper.UpdateDtoMapper;
import org.ubiquia.common.library.implementation.service.visitor.PageValidator;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.dto.Update;
import org.ubiquia.common.model.ubiquia.entity.UpdateEntity;

/**
 * A controller that exposes a RESTful interface to get updates for a given model.
 */
@RestController
@RequestMapping("/ubiquia/core/flow-service/update")
public class UpdateController implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(UpdateController.class);

    protected final Pattern camelcaseRegex;

    @Autowired
    private EntityDao<UpdateEntity> entityDao;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UpdateDtoMapper updateDtoMapper;
    @Autowired
    private PageValidator pageValidator;

    public UpdateController() {

        this.getLogger().info("Initializing...");

        this.camelcaseRegex = Pattern.compile("-([a-z])");

        this.getLogger().info("...initialized...");
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @GetMapping("/query/params")
    public GenericPageImplementation<Update> queryWithParams(
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
     * @return A page of converted DTO's.
     * @throws JsonProcessingException Exceptions from parsing database records.
     */
    protected GenericPageImplementation<Update> convertPageHelper(
        final Page<UpdateEntity> records)
        throws JsonProcessingException {

        GenericPageImplementation<Update> convertedPage;
        var converted = this.updateDtoMapper.map(records.getContent());
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
    protected GenericPageImplementation<Update> query(
        final Integer page,
        final Integer size,
        final Boolean sortDescending,
        final List<String> sortByFields,
        final HttpServletRequest httpServletRequest)
        throws NoSuchFieldException, JsonProcessingException {

        this.pageValidator.validatePageAndSize(page, size);
        var map = this.getMapFromServletRequest(httpServletRequest);
        this.getLogger().info("Received a query request by params...");

        var records = this.entityDao.getPage(
            map,
            page,
            size,
            sortDescending,
            sortByFields,
            UpdateEntity.class);

        var egress = this.convertPageHelper(records);
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
