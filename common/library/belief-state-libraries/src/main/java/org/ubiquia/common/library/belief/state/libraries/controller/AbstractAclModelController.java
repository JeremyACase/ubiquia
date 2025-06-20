package org.ubiquia.common.library.belief.state.libraries.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.library.api.service.builder.IngressResponseBuilder;
import org.ubiquia.common.library.api.service.visitor.PageValidator;
import org.ubiquia.common.library.belief.state.libraries.interfaces.InterfaceModelController;
import org.ubiquia.common.library.belief.state.libraries.model.association.Association;
import org.ubiquia.common.library.belief.state.libraries.service.EntityUpdater;
import org.ubiquia.common.library.belief.state.libraries.service.builder.io.DtoPageBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.builder.telemetry.MicroMeterTagsBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.command.MicroMeterCommand;
import org.ubiquia.common.library.belief.state.libraries.service.finder.EntityRepositoryFinder;
import org.ubiquia.common.library.belief.state.libraries.service.logic.AclControllerLogic;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.model.acl.dto.AbstractAclEntityDto;
import org.ubiquia.common.model.acl.embeddable.KeyValuePair;
import org.ubiquia.common.model.acl.entity.AbstractAclEntity;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.IngressResponse;

public abstract class AbstractAclModelController<
    T extends AbstractAclEntity,
    D extends AbstractAclEntityDto>
    implements InterfaceModelController<T, D>,
    InterfaceLogger {

    protected List<KeyValuePair> tags;
    protected Class<T> cachedEntityClass;
    protected Class<D> cachedDtoClass;
    @Autowired
    protected AclControllerLogic aclControllerLogic;
    @Autowired
    protected EntityDao<T> entityDao;
    @Autowired
    protected EntityRepositoryFinder entityRepositoryFinder;
    @Autowired
    protected EntityUpdater entityUpdater;
    @Autowired
    protected DtoPageBuilder<T, D> dtoPageBuilder;
    @Autowired
    protected IngressResponseBuilder ingressResponseBuilder;
    @Autowired(required = false)
    protected MicroMeterCommand microMeterCommand;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected PageValidator pageValidator;
    @Autowired
    private MicroMeterTagsBuilder microMeterTagsBuilder;

    @SuppressWarnings("unchecked")
    public AbstractAclModelController() {
        this.getLogger().info("Initializing...");

        this.cachedEntityClass = (Class<T>) ((ParameterizedType) this.getClass()
            .getGenericSuperclass()).getActualTypeArguments()[0];
        this.cachedDtoClass = (Class<D>) ((ParameterizedType) this.getClass()
            .getGenericSuperclass()).getActualTypeArguments()[1];

        this.getLogger().info("...initialized...");
    }

    /**
     * Post-application start method to manage some initialization.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStart() {
        this.getLogger().info("Processing post-startup initialization...");
        this.tags = this.microMeterTagsBuilder.buildControllerTagsFor(this);
        this.getLogger().info("...initialized...");
    }

    @Transactional
    @PostMapping(value = "/add")
    public IngressResponse add(
        @RequestBody D ingress,
        @RequestParam(value = "message-source", defaultValue = "belief-state") String messageSource)
        throws Exception {

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterCommand)) {
            sample = this.microMeterCommand.startSample();
        }

        this.getLogger().info("Received a model to persist of type {}...",
            ingress.getModelType());
        if (Objects.nonNull(ingress.getId())) {
            throw new IllegalArgumentException("ERROR: Payload has an id during an add; "
                + " id's must be null for adds!");
        }
        var entity = this.getIngressMapper().map(ingress, this.cachedEntityClass);
        entity = this.getEntityRepository().save(entity);
        this.getEntityRelationshipBuilder().tryBuildRelationships(entity);
        var response = this.ingressResponseBuilder.buildIngressResponseFor(entity);
        if (Objects.nonNull(sample)) {
            this.microMeterCommand.endSample(sample, "add", this.tags);
        }

        return response;
    }

    @PostMapping(value = "/add/list")
    @Transactional
    public List<IngressResponse> addList(
        @RequestBody List<D> ingresses,
        @RequestParam(value = "message-source", defaultValue = "belief-state") String messageSource)
        throws Exception {

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterCommand)) {
            sample = this.microMeterCommand.startSample();
        }
        this.getLogger().info("Received a list of models to persist...");

        var convertedEntities = new ArrayList<T>();
        for (var ingress : ingresses) {
            this.getLogger().debug("Processing model of type of type {}...",
                ingress.getModelType());
            var converted = this.getIngressMapper().map(ingress, this.cachedEntityClass);
            if (Objects.nonNull(converted.getId())) {
                throw new IllegalArgumentException("ERROR: Payload has an id during an add; "
                    + " id's must be null for adds!");
            }
            convertedEntities.add(converted);
        }

        var responses = new ArrayList<IngressResponse>();
        var entities = this.getEntityRepository().saveAll(convertedEntities);
        for (var entity : entities) {
            this.getEntityRelationshipBuilder().tryBuildRelationships(entity);
            var response = this.ingressResponseBuilder.buildIngressResponseFor(entity);
            responses.add(response);
        }

        if (Objects.nonNull(sample)) {
            this.microMeterCommand.endSample(sample, "addList", this.tags);
        }

        return responses;
    }

    @PostMapping(value = "/associate")
    @Transactional
    public IngressResponse associate(
        @RequestBody @Validated Association association,
        @RequestParam(value = "message-source", defaultValue = "belief-state") String messageSource)
        throws Exception {

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterCommand)) {
            sample = this.microMeterCommand.startSample();
        }

        this.getLogger().info("Received an association request: \n{}",
            this.objectMapper.writeValueAsString(association));

        var entityFields = Arrays.stream(FieldUtils.getAllFields(this.cachedEntityClass))
            .toList();

        var match = entityFields.stream().filter(x -> x
                .getName()
                .equals(association.getParentAssociation().getFieldName()))
            .findFirst();

        if (match.isEmpty()) {
            throw new IllegalArgumentException("ERROR: Could not find field named "
                + association.getParentAssociation().getFieldName()
                + " in the model of type "
                + this.cachedEntityClass.getSimpleName());
        }

        var parentRepository = this.getEntityRepository();
        var parentRecord = parentRepository.findById(association.getParentAssociation().getParentId());

        if (parentRecord.isEmpty()) {
            throw new IllegalArgumentException("Cannot find parent record with id: "
                + association.getParentAssociation().getParentId());
        }

        var parentEntity = parentRecord.get();
        var childField = match.get();
        var childEntity = this.getChildRecord(childField, association);
        childEntity.setId(association.getChildAssociation().getChildId());
        this.associateParentAndChild(childField, parentEntity, childEntity);
        parentEntity = parentRepository.save(parentEntity);
        this.getEntityRelationshipBuilder().tryBuildRelationships(parentEntity);

        var response = this.ingressResponseBuilder.buildIngressResponseFor(parentEntity);
        if (Objects.nonNull(sample)) {
            this.microMeterCommand.endSample(sample, "associate", this.tags);
        }

        return response;
    }

    @PostMapping(value = "/tag/add/{id}")
    @Transactional
    public IngressResponse addTag(
        @PathVariable("id") final String id,
        @RequestBody KeyValuePair tag,
        @RequestParam(value = "message-source", defaultValue = "belief-state") String messageSource) {

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterCommand)) {
            sample = this.microMeterCommand.startSample();
        }

        this.getLogger().info("Received a tag to add: {}...", tag);
        var record = this.getEntityRepository().findById(id);
        if (record.isEmpty()) {
            throw new IllegalArgumentException("Cannot find record with id: " + id);
        }
        var entity = record.get();

        if (Objects.isNull(entity.getTags())) {
            entity.setTags(new HashSet<>());
        }
        entity.getTags().add(tag);

        entity = this.getEntityRepository().save(entity);
        var response = this.ingressResponseBuilder.buildIngressResponseFor(entity);

        if (Objects.nonNull(sample)) {
            this.microMeterCommand.endSample(sample, "addTag", this.tags);
        }

        return response;
    }

    @PostMapping(value = "/tag/remove/{id}")
    @Transactional
    public IngressResponse removeTag(
        @PathVariable("id") final String id,
        @RequestBody KeyValuePair tag,
        @RequestParam(value = "message-source", defaultValue = "belief-state") String messageSource) {

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterCommand)) {
            sample = this.microMeterCommand.startSample();
        }

        this.getLogger().info("Received a tag to remove: {}...", tag);
        var record = this.getEntityRepository().findById(id);
        if (record.isEmpty()) {
            throw new IllegalArgumentException("Cannot find record with id: " + id);
        }
        var entity = record.get();

        if (Objects.isNull(entity.getTags())) {
            throw new IllegalArgumentException("ERROR: Could not find a tag with key: "
                + tag.getKey());
        }
        var match = entity
            .getTags()
            .stream()
            .filter(x -> x.getKey().equals(tag.getKey()))
            .findFirst();
        if (match.isEmpty()) {
            throw new IllegalArgumentException("ERROR: Could not find a tag with key: "
                + tag.getKey());
        }
        entity.getTags().remove(match.get());
        entity = this.getEntityRepository().save(entity);

        var response = this.ingressResponseBuilder.buildIngressResponseFor(entity);
        if (Objects.nonNull(sample)) {
            this.microMeterCommand.endSample(sample, "removeTag", this.tags);
        }
        return response;
    }

    @GetMapping("/tags/get/keys")
    public List<String> getDistinctKeys() {
        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterCommand)) {
            sample = this.microMeterCommand.startSample();
        }
        this.getLogger().info("Received request to get distinct keys...");
        var keys = this.getEntityRepository().findAllDistinctTagKeys();
        if (Objects.nonNull(sample)) {
            this.microMeterCommand.endSample(sample, "getDistinctKeys", this.tags);
        }
        return keys;
    }

    @GetMapping("/tags/get/values-by-key/{key}")
    public List<String> getDistinctValuesByKey(@PathVariable("key") String key) {
        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterCommand)) {
            sample = this.microMeterCommand.startSample();
        }
        this.getLogger().info("Received request to get distinct values for key: {}", key);
        var values = this.getEntityRepository().findAllDistinctTagValuesByKey(key);
        if (Objects.nonNull(sample)) {
            this.microMeterCommand.endSample(sample, "valuesByKey", this.tags);
        }
        return values;
    }

    @SuppressWarnings("checkstyle:Indentation")
    @Transactional
    @PostMapping("/update/field/{id}")
    public IngressResponse updateField(
        @PathVariable String id,
        @RequestBody List<KeyValuePair> keyValuePairs,
        @RequestParam(value = "message-source", defaultValue = "belief-state") String messageSource)
        throws IllegalArgumentException,
        IllegalAccessException {

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterCommand)) {
            sample = this.microMeterCommand.startSample();
        }

        this.getLogger().info("Received update request for model with id: {}", id);
        this.getLogger().debug("Key Value Pairs for update: {}", keyValuePairs);

        if (Objects.isNull(id)) {
            throw new IllegalArgumentException("ERROR: Cannot update a model with a null id!");
        }

        var record = this.getEntityRepository().findById(id);
        if (record.isEmpty()) {
            throw new IllegalArgumentException("ERROR: Cannot find a record with the id: " + id);
        }

        var entity = record.get();
        this.entityUpdater.trySetValue(entity, keyValuePairs);
        entity = this.getEntityRepository().save(entity);

        var response = this.ingressResponseBuilder.buildIngressResponseFor(entity);

        if (Objects.nonNull(sample)) {
            this.microMeterCommand.endSample(sample, "update.field", this.tags);
        }
        return response;
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteModel(@PathVariable("id") final String id) {
        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterCommand)) {
            sample = this.microMeterCommand.startSample();
        }
        this.getLogger().info("Received request to delete ID {}...", id);
        ResponseEntity<String> response = null;
        var record = this.getEntityRepository().findById(id);
        if (record.isEmpty()) {
            response = ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } else {
            response = ResponseEntity.status(HttpStatus.OK).body(record.get().getId());
            this.getEntityRepository().delete(record.get());
        }
        if (Objects.nonNull(sample)) {
            this.microMeterCommand.endSample(sample, "delete", this.tags);
        }
        return response;
    }

    /**
     * Query for an individual models.
     *
     * @param id The ID of the model to query.
     * @return The record if applicable, otherwise a 204.
     * @throws NoSuchFieldException   Exception from missing fields.
     * @throws IllegalAccessException Exception from illegally accessing fields.
     */
    @GetMapping("/query/{id}")
    @Transactional
    public ResponseEntity<D> queryModelWithId(@PathVariable("id") final String id)
        throws NoSuchFieldException, IllegalAccessException {

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterCommand)) {
            sample = this.microMeterCommand.startSample();
        }

        this.getLogger().info("Received a GET request for ID: {}", id);

        var parameterMap = new HashMap<String, String[]>();
        var array = new String[1];
        array[0] = id;
        parameterMap.put("id", array);

        var records = this.entityDao.getPage(
            parameterMap,
            0,
            1,
            true,
            new ArrayList<>(),
            this.cachedEntityClass);

        ResponseEntity<D> response = null;

        if (records.isEmpty()) {
            response = ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } else {
            var egress = this.dtoPageBuilder.buildPageFrom(records);
            response = ResponseEntity.status(HttpStatus.OK).body(egress.getContent().get(0));
        }

        if (Objects.nonNull(sample)) {
            this.microMeterCommand.endSample(sample, "queryWithId", this.tags);
        }

        return response;
    }

    /**
     * Query for the number of records applicable for a set of predicates.
     *
     * @param httpServletRequest The request to translate into a set of predicates.
     * @return The number of applicable records.
     * @throws NoSuchFieldException Exception from passing in invalid predicates.
     */
    @GetMapping("/query/count/params")
    @Transactional
    public Long queryCountWithParams(final HttpServletRequest httpServletRequest)
        throws NoSuchFieldException {

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterCommand)) {
            sample = this.microMeterCommand.startSample();
        }

        this.getLogger().info("Received a count request with params...");

        var parameterMap = this.aclControllerLogic.getParameterMapFrom(httpServletRequest);
        var count = this.entityDao.getCount(parameterMap, this.cachedEntityClass);

        if (Objects.nonNull(sample)) {
            this.microMeterCommand.endSample(sample, "queryModels", this.tags);
        }

        return count;
    }

    @GetMapping("/query/multiselect/params")
    @Transactional
    public Page<Object[]> queryWithMultiselectParams(
        @RequestParam("page") final Integer page,
        @RequestParam("size") final Integer size,
        @RequestParam(value = "sort-descending", required = false, defaultValue = "true") final Boolean sortDescending,
        @RequestParam(value = "sort-by-fields", required = false, defaultValue = "") final List<String> sortByFields,
        @RequestParam(value = "multiselect-fields", defaultValue = "") List<String> multiselectFields,
        HttpServletRequest httpServletRequest) throws
        NoSuchFieldException {

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterCommand)) {
            sample = this.microMeterCommand.startSample();
        }

        this.getLogger().debug("Received a multiselect query request...");

        this.pageValidator.validatePageAndSize(page, size);
        var parameterMap = this.aclControllerLogic.getParameterMapFrom(httpServletRequest);

        var records = this.entityDao.getPageMultiselect(
            parameterMap,
            page,
            size,
            sortDescending,
            sortByFields,
            multiselectFields,
            this.cachedEntityClass);

        if (Objects.nonNull(sample)) {
            this.microMeterCommand.endSample(sample, "queryModels", this.tags);
        }

        return records;
    }

    @GetMapping("/query/params")
    @Transactional
    public GenericPageImplementation<D> queryWithParams(
        @RequestParam("page") final Integer page,
        @RequestParam("size") final Integer size,
        @RequestParam(value = "sort-descending", required = false, defaultValue = "true") final Boolean sortDescending,
        @RequestParam(value = "sort-by-fields", required = false, defaultValue = "") final List<String> sortByFields,
        HttpServletRequest httpServletRequest) throws
        NoSuchFieldException, IllegalAccessException {

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterCommand)) {
            sample = this.microMeterCommand.startSample();
        }

        this.getLogger().info("Received a query request by params...");

        this.pageValidator.validatePageAndSize(page, size);
        var parameterMap = this.aclControllerLogic.getParameterMapFrom(httpServletRequest);

        var records = this.entityDao.getPage(
            parameterMap,
            page,
            size,
            sortDescending,
            sortByFields,
            this.cachedEntityClass);

        var egress = this.dtoPageBuilder.buildPageFrom(records);

        if (Objects.nonNull(sample)) {
            this.microMeterCommand.endSample(sample, "queryModels", this.tags);
        }

        return egress;
    }

    @SuppressWarnings("unchecked")
    private void associateParentAndChild(
        Field childField,
        AbstractAclEntity parentEntity,
        AbstractAclEntity childEntity)
        throws IllegalAccessException {

        if (List.class.isAssignableFrom(childField.getType())) {
            var list = (List<AbstractAclEntity>) childField.get(parentEntity);
            if (Objects.isNull(list)) {
                list = new ArrayList<>();
                childField.set(parentEntity, list);
            }
            list.add(childEntity);
        } else if (Set.class.isAssignableFrom(childField.getType())) {
            var set = (Set<AbstractAclEntity>) childField.get(parentEntity);
            if (Objects.isNull(set)) {
                set = new HashSet<>();
                childField.set(parentEntity, set);
            }
            set.add(childEntity);
        } else {
            childField.set(parentEntity, childEntity);
        }
    }

    /**
     * Get a child record from a field towards associating it with a parent record.
     *
     * @param childField  The child field.
     * @param association The association to make.
     * @return A child record if one is present in the database.
     */
    @SuppressWarnings("unchecked")
    private AbstractAclEntity getChildRecord(
        final Field childField,
        final Association association) {

        childField.setAccessible(true);

        String childFieldName = null;
        if (List.class.isAssignableFrom(childField.getType())
            || Set.class.isAssignableFrom(childField.getType())) {
            var elementType = (ParameterizedType) childField.getGenericType();
            var elementClass = (Class<?>) elementType.getActualTypeArguments()[0];
            childFieldName = elementClass.getSimpleName();
        } else {
            childFieldName = childField.getType().getSimpleName();
        }

        var childRepository = this.entityRepositoryFinder.findRepositoryFor(childFieldName);

        var childRecord = childRepository.findById(
            association.getChildAssociation().getChildId());

        if (childRecord.isEmpty()) {
            throw new IllegalArgumentException("ERROR: Could not find child ID for id: "
                + association.getChildAssociation().getChildId());
        }

        var childEntity = (AbstractAclEntity) childRecord.get();
        return childEntity;
    }
}
