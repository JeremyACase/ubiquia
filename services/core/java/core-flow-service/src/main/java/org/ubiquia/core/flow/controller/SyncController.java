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
import org.ubiquia.common.library.implementation.service.mapper.SyncDtoMapper;
import org.ubiquia.common.library.implementation.service.visitor.PageValidator;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.dto.Sync;
import org.ubiquia.common.model.ubiquia.entity.SyncEntity;

@RestController
@RequestMapping("/ubiquia/core/flow-service/sync")
public class SyncController implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(SyncController.class);

    protected final Pattern camelcaseRegex;

    @Autowired
    private EntityDao<SyncEntity> entityDao;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SyncDtoMapper syncDtoMapper;

    @Autowired
    private PageValidator pageValidator;

    public SyncController() {
        this.getLogger().info("Initializing...");
        this.camelcaseRegex = Pattern.compile("-([a-z])");
        this.getLogger().info("...initialized...");
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @GetMapping("/query/params")
    public GenericPageImplementation<Sync> queryWithParams(
        @RequestParam("page") final Integer page,
        @RequestParam("size") final Integer size,
        @RequestParam(value = "sort-descending", required = false, defaultValue = "true") final Boolean sortDescending,
        @RequestParam(value = "sort-by-fields", required = false, defaultValue = "") final List<String> sortByFields,
        HttpServletRequest httpServletRequest)
        throws NoSuchFieldException, JsonProcessingException {

        this.pageValidator.validatePageAndSize(page, size);
        var map = this.getMapFromServletRequest(httpServletRequest);
        this.getLogger().info("Received a sync query request by params...");

        var records = this.entityDao.getPage(
            map,
            page,
            size,
            sortDescending,
            sortByFields,
            SyncEntity.class);

        return this.convertPageHelper(records);
    }

    private GenericPageImplementation<Sync> convertPageHelper(final Page<SyncEntity> records)
        throws JsonProcessingException {

        var converted = this.syncDtoMapper.map(records.getContent());
        return new GenericPageImplementation<>(
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
    }

    private HashMap<String, String[]> getMapFromServletRequest(HttpServletRequest request) {
        var map = new HashMap<String, String[]>();
        for (var key : request.getParameterMap().keySet()) {
            var camelCase = this.camelcaseRegex.matcher(key)
                .replaceAll(x -> x.group(1).toUpperCase());
            map.put(camelCase, request.getParameterMap().get(key));
        }
        map.remove("page");
        map.remove("size");
        map.remove("sortDescending");
        map.remove("sortByFields");
        return map;
    }
}
