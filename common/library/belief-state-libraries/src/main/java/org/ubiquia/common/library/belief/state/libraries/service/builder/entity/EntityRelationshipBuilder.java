package org.ubiquia.common.library.belief.state.libraries.service.builder.entity;

import jakarta.transaction.Transactional;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.library.belief.state.libraries.service.finder.EntityRelationshipBuilderFinder;
import org.ubiquia.common.library.belief.state.libraries.service.finder.EntityRepositoryFinder;
import org.ubiquia.common.model.acl.entity.AbstractAclModelEntity;

@Service
public abstract class EntityRelationshipBuilder<T extends AbstractAclModelEntity>
    implements InterfaceLogger {

    protected final HashMap<Field, Field> cachedEntityFieldMap;

    protected final HashMap<Field, Field> cachedEntityListFieldMap;

    protected final HashMap<Field, Field> cachedEntitySetFieldMap;

    protected final Class<T> cachedEntityClass;

    @Autowired
    private EntityRelationshipBuilderFinder entityRelationshipBuilderFinder;

    @Autowired
    private EntityRepositoryFinder repositoryFinder;

    /**
     * Abandon all hope, ye who enter here.
     */
    @SuppressWarnings("unchecked")
    public EntityRelationshipBuilder() {
        this.getLogger().info("Caching reflection data...");

        // Cache our persistent class in derived classes.
        this.cachedEntityClass = (Class<T>) ((ParameterizedType) this.getClass()
            .getGenericSuperclass()).getActualTypeArguments()[0];

        this.cachedEntityFieldMap = new HashMap<>();
        this.cachedEntityListFieldMap = new HashMap<>();
        this.cachedEntitySetFieldMap = new HashMap<>();

        // Loop through all of the fields of our current class....
        var fields = FieldUtils.getAllFields(this.cachedEntityClass);
        for (var field : fields) {

            var cached = this.tryCacheFieldRelationship(field);
            if (!cached) {
                cached = this.tryCacheListFieldRelationship(field);

                if (!cached) {
                    this.tryCacheSetFieldRelationship(field);
                }
            }
        }

        this.getLogger().info("...finished caching reflection data.");
    }

    /**
     * Attempt to set any relationships found in a model by using the appropriate controller bean.
     *
     * @param model The model to set relationships for.
     * @throws IllegalAccessException Reflection exceptions.
     */
    @SuppressWarnings("unchecked")
    public void tryBuildRelationships(T model) throws Exception {

        var entityName = this.getEntityClassName(model);

        if (!this.cachedEntityClass.getName().equalsIgnoreCase(entityName)) {
            var mapperBean = this.tryGetRelationshipBuilder(model);
            if (Objects.nonNull(mapperBean)) {
                mapperBean.tryBuildRelationships(model);
            } else {
                this.trySetRelationships(model);
            }
        } else {
            this.trySetRelationships(model);
        }
    }

    /**
     * Given a model, attempt to get retrieve a bean that knows how to map its relationships.
     *
     * @param model The model to retrieve a mapper for.
     * @return The mapper if found, else null.
     */
    @SuppressWarnings("rawtypes")
    private EntityRelationshipBuilder tryGetRelationshipBuilder(
        final AbstractAclModelEntity model) {

        EntityRelationshipBuilder relationshipBuilder = null;
        var entityName = this.getEntityClassName(model);

        if (!entityName.equalsIgnoreCase(this.cachedEntityClass.getSimpleName())) {
            relationshipBuilder = this
                .entityRelationshipBuilderFinder
                .findRelationshipBuilderFor(model);
        }
        return relationshipBuilder;
    }

    /**
     * Given a model and a field which is itself a MACHINA database entity, set the bidirectional
     * relationship and persist in the database.
     *
     * @param model The parent model.
     * @param key   The key representing the field of the child model.
     * @param value The actual value of the field.
     * @throws Exception Exceptions.
     */
    @SuppressWarnings("unchecked")
    @Transactional
    private void setBidirectionalEntityRelationship(T model, Field key, Object value)
        throws Exception {
        if (Objects.nonNull(value)) {

            // Query the ID field from the current object of our model...
            var idField = Arrays.stream(FieldUtils.getAllFields(value.getClass())).filter(x ->
                    x.getName()
                        .equals("id"))
                .findFirst()
                .get();
            idField.setAccessible(true);
            var idValue = idField.get(value);

            var field = this.cachedEntityFieldMap.get(key);
            field.setAccessible(true);
            var repository = this.repositoryFinder.findRepositoryFor(value);

            // ...if we find it, fetch the entire object from our database...
            if (Objects.nonNull(idValue)) {
                this.getLogger().debug("Found ID {} for field {}; retrieving corresponding repository...",
                    idValue,
                    key.getName());
                var fieldValueRecord = repository.findById(idValue);
                if (fieldValueRecord.isPresent()) {
                    var fieldValue = fieldValueRecord.get();
                    this.getLogger().debug("Found model with ID {}; establishing relationship...",
                        idValue);

                    // ...now set the child field's value to point to the parent model...
                    if (List.class.isAssignableFrom(field.getType())) {
                        var list = (List<AbstractAclModelEntity>) field.get(fieldValue);
                        if (Objects.isNull(list)) {
                            list = new ArrayList<>();
                            field.set(fieldValue, list);
                        }
                        list.add(model);
                    } else if (Set.class.isAssignableFrom(field.getType())) {
                        var set = (Set<AbstractAclModelEntity>) field.get(fieldValue);
                        if (Objects.isNull(set)) {
                            set = new HashSet<>();
                            field.set(fieldValue, set);
                        }
                        set.add(model);
                    } else {
                        field.set(fieldValue, model);
                    }
                    fieldValue = repository.save(fieldValue);

                    // ...and set the parent model to the fully-hydrated child object...
                    key.set(model, fieldValue);
                } else {
                    throw new IllegalArgumentException("ERROR: Could not find model with id: "
                        + idValue);
                }
            } else {
                this.getLogger().debug("Child element of type {} of field {} did not have ID field; "
                        + "persisting in database...",
                    value.getClass().getSimpleName(),
                    key.getName());

                var entity = (AbstractAclModelEntity) value;
                var entityName = this.getEntityClassName(model);

                if (!entityName.equalsIgnoreCase(this.cachedEntityClass.getSimpleName())) {
                    var mapperBean = this.tryGetRelationshipBuilder(entity);
                    if (Objects.nonNull(mapperBean)) {
                        mapperBean.tryBuildRelationships(entity);
                    }
                }

                // ...now set the child field's value to point to the parent model...
                if (List.class.isAssignableFrom(field.getType())) {
                    var list = (List<AbstractAclModelEntity>) field.get(value);
                    if (Objects.isNull(list)) {
                        list = new ArrayList<>();
                        field.set(value, list);
                    }
                    list.add(model);
                }  else if (Set.class.isAssignableFrom(field.getType())) {
                    var set = (Set<AbstractAclModelEntity>) field.get(value);
                    if (Objects.isNull(set)) {
                        set = new HashSet<>();
                        field.set(value, set);
                    }
                    set.add(model);
                } else {
                    field.set(value, model);
                }
                value = repository.save(value);

                key.set(model, value);
                this.getLogger().debug("...successfully persisted new record of type {}...",
                    value.getClass().getSimpleName());
            }
        }
    }

    /**
     * Given a parentModel and a field which is itself a MACHINA database entity, set the bidirectional
     * relationship and persist in the database.
     *
     * @param parentModel     The parent parentModel.
     * @param key             The key representing the field of the child parentModel.
     * @param childModelValue The actual childModelValue of the field.
     * @throws IllegalAccessException Reflection exceptions.
     */
    @Transactional
    private void setBidirectionalListRelationship(T parentModel, Field key, Object childModelValue)
        throws Exception {
        if (Objects.nonNull(childModelValue)) {

            // Query the ID field from the current object of our parentModel...
            var idField = Arrays.stream(FieldUtils.getAllFields(childModelValue.getClass())).filter(x ->
                    x.getName()
                        .equals("id"))
                .findFirst()
                .get();
            idField.setAccessible(true);
            var idValue = idField.get(childModelValue);

            var repository = this.repositoryFinder.findRepositoryFor(childModelValue);
            var field = this.cachedEntityListFieldMap.get(key);
            field.setAccessible(true);

            // ...if we find it, fetch the entire object from our database...
            if (Objects.nonNull(idValue)) {
                this.getLogger().debug("Found ID {} for field {}; retrieving corresponding repository...",
                    idValue,
                    key.getName());

                var fieldModelRecord = repository.findById(idValue);
                if (fieldModelRecord.isPresent()) {
                    this.getLogger().debug("Found parentModel with ID {}; establishing relationship...",
                        idValue);

                    // ...now set the child field's childModelValue to point to the parent parentModel...
                    if (List.class.isAssignableFrom(field.getType())) {
                        var childList = (List<AbstractAclModelEntity>) field.get(childModelValue);
                        if (Objects.isNull(childList)) {
                            childList = new ArrayList<>();
                            field.set(childModelValue, childList);
                        }
                        childList.add(parentModel);
                    } else {
                        // ...and set the parent parentModel to the fully-hydrated child object...
                        field.set(fieldModelRecord.get(), parentModel);
                    }
                    // Save our new parentModel and re-add the hydrated object back into the parent's childList.
                    var childList = (List<Object>) key.get(parentModel);
                    childList.add(childModelValue);
                    childModelValue = repository.save(childModelValue);
                } else {
                    throw new IllegalArgumentException("ERROR: Could not query parentModel with id: "
                        + idValue);
                }
            } else {
                this.getLogger().debug("Child element of type {} of list field {} did not have ID field; "
                        + "persisting in database...",
                    childModelValue.getClass().getSimpleName(),
                    key.getName());

                var entity = (AbstractAclModelEntity) childModelValue;
                var entityName = this.getEntityClassName(entity);
                if (!entityName.equalsIgnoreCase(this.cachedEntityClass.getSimpleName())) {
                    var mapperBean = this.tryGetRelationshipBuilder(entity);
                    if (Objects.isNull(mapperBean)) {
                        throw new Exception("ERROR: Could not find a relationship mapper for "
                            + entity.getModelType());
                    }
                    mapperBean.tryBuildRelationships(entity);
                }

                // ...now set the child field's childModelValue to point to the parent parentModel...
                if (List.class.isAssignableFrom(field.getType())) {
                    var list = (List<AbstractAclModelEntity>) field.get(childModelValue);
                    if (Objects.isNull(list)) {
                        list = new ArrayList<>();
                        field.set(childModelValue, list);
                    }
                    list.add(parentModel);

                } else {
                    field.set(childModelValue, parentModel);
                }

                // Save our new parentModel and re-add the hydrated object back into the parent's list.
                var list = (List<Object>) key.get(parentModel);
                list.add(childModelValue);
                childModelValue = repository.save(childModelValue);

                this.getLogger().debug("...successfully persisted new record of type {}...",
                    childModelValue.getClass().getSimpleName());
            }
            this.getLogger().debug("...successfully saved relationship.");
        }
    }

    @Transactional
    private void setBidirectionalSetRelationship(T parentModel, Field key, Object childModelValue)
        throws Exception {
        if (Objects.nonNull(childModelValue)) {

            // Query the ID field from the current object of our parentModel...
            var idField = Arrays.stream(FieldUtils.getAllFields(childModelValue.getClass())).filter(x ->
                    x.getName()
                        .equals("id"))
                .findFirst()
                .get();
            idField.setAccessible(true);
            var idValue = idField.get(childModelValue);

            var repository = this.repositoryFinder.findRepositoryFor(childModelValue);
            var field = this.cachedEntitySetFieldMap.get(key);
            field.setAccessible(true);

            // ...if we find it, fetch the entire object from our database...
            if (Objects.nonNull(idValue)) {
                this.getLogger().debug("Found ID {} for field {}; retrieving corresponding repository...",
                    idValue,
                    key.getName());

                var fieldModelRecord = repository.findById(idValue);
                if (fieldModelRecord.isPresent()) {
                    this.getLogger().debug("Found parentModel with ID {}; establishing relationship...",
                        idValue);

                    // ...now set the child field's childModelValue to point to the parent parentModel...
                    if (Set.class.isAssignableFrom(field.getType())) {
                        var childSet = (Set<AbstractAclModelEntity>) field.get(childModelValue);
                        if (Objects.isNull(childSet)) {
                            childSet = new HashSet<>();
                            field.set(childModelValue, childSet);
                        }
                        childSet.add(parentModel);
                    } else {
                        // ...and set the parent parentModel to the fully-hydrated child object...
                        field.set(fieldModelRecord.get(), parentModel);
                    }
                    // Save our new parentModel and re-add the hydrated object back into the parent's childList.
                    var childSet = (Set<Object>) key.get(parentModel);
                    childSet.add(childModelValue);
                    childModelValue = repository.save(childModelValue);
                } else {
                    throw new IllegalArgumentException("ERROR: Could not query parentModel with id: "
                        + idValue);
                }
            } else {
                this.getLogger().debug("Child element of type {} of list field {} did not have ID field; "
                        + "persisting in database...",
                    childModelValue.getClass().getSimpleName(),
                    key.getName());

                var entity = (AbstractAclModelEntity) childModelValue;
                var entityClassName = entity.getModelType();
                if (!entityClassName.equalsIgnoreCase(this.cachedEntityClass.getSimpleName())) {
                    var mapperBean = this.tryGetRelationshipBuilder(entity);
                    if (Objects.isNull(mapperBean)) {
                        throw new Exception("ERROR: Could not find a relationship mapper for "
                            + entity.getModelType());
                    }
                    mapperBean.tryBuildRelationships(entity);
                }

                // ...now set the child field's childModelValue to point to the parent parentModel...
                if (Set.class.isAssignableFrom(field.getType())) {
                    var set = (Set<AbstractAclModelEntity>) field.get(childModelValue);
                    if (Objects.isNull(set)) {
                        set = new HashSet<>();
                        field.set(childModelValue, set);
                    }
                    set.add(parentModel);

                } else {
                    field.set(childModelValue, parentModel);
                }

                // Save our new parentModel and re-add the hydrated object back into the parent's list.
                var set = (Set<Object>) key.get(parentModel);
                set.add(childModelValue);
                childModelValue = repository.save(childModelValue);

                this.getLogger().debug("...successfully persisted new record of type {}...",
                    childModelValue.getClass().getSimpleName());
            }
            this.getLogger().debug("...successfully saved relationship.");
        }
    }

    /**
     * Attempt to set any relationships found in a model.
     *
     * @param model The model to set relationships for.
     * @throws IllegalAccessException Reflection exceptions.
     */
    private void trySetRelationships(T model) throws Exception {

        for (var key : this.cachedEntityFieldMap.keySet()) {
            var value = key.get(model);
            this.setBidirectionalEntityRelationship(model, key, value);
        }

        var listMap = new HashMap<Field, List<Object>>();
        for (var key : this.cachedEntityListFieldMap.keySet()) {
            listMap.put(key, (List<Object>) key.get(model));
            key.set(model, new ArrayList<>());
        }


        for (var key : this.cachedEntityListFieldMap.keySet()) {
            var values = (List<Object>) listMap.get(key);

            if (Objects.nonNull(values)) {
                for (var value : values) {
                    this.setBidirectionalListRelationship(model, key, value);
                }
            }
        }

        var setMap = new HashMap<Field, Set<Object>>();
        for (var key : this.cachedEntitySetFieldMap.keySet()) {
            setMap.put(key, (Set<Object>) key.get(model));
            key.set(model, new HashSet<>());
        }

        for (var key : this.cachedEntitySetFieldMap.keySet()) {
            var values = (Set<Object>) setMap.get(key);

            if (Objects.nonNull(values)) {
                for (var value : values) {
                    this.setBidirectionalSetRelationship(model, key, value);
                }
            }
        }
    }

    /**
     * Attempt to cache any fields for this model with bidirectional database relationships.
     *
     * @param field The field of the model we're checking relationships for.
     * @return Whether or not we were able to cache the relationship.
     */
    private Boolean tryCacheFieldRelationship(final Field field) {
        var cached = false;

        // ...determine if any of those fields reference this model via a relationship...
        if (AbstractAclModelEntity.class.isAssignableFrom(field.getType())) {
            cached = this.tryCacheEntityRelationship(field);
        }
        return cached;
    }

    /**
     * Determine whether or not a field is an entity; if it is, cache it for further
     * use in mapping.
     *
     * @param field The field to attempt to cache.
     * @return Whether the field was cached or not.
     */
    private Boolean tryCacheEntityRelationship(final Field field) {
        var cached = false;

        var subFields = Arrays.stream(FieldUtils.getAllFields(field.getType())).toList();
        var fieldReference = subFields.stream().filter(x ->
            x.getType().isAssignableFrom(this.cachedEntityClass)).findFirst();

        // ...if we have a 1:1 relationship, cache it...
        if (fieldReference.isPresent()) {
            this.getLogger().debug("Field named {} contains this model {} as a 1:1 relationship via field {}",
                field.getName(),
                this.cachedEntityClass.getSimpleName(),
                fieldReference.get().getName());
            field.setAccessible(true);
            fieldReference.get().setAccessible(true);
            this.cachedEntityFieldMap.put(field, fieldReference.get());
            cached = true;

            // ...otherwise check to see if the child field has a 1:many relationship with this model...
        } else {
            var listFields = subFields.stream().filter(x ->
                List.class.isAssignableFrom(x.getType())).toList();
            for (var listField : listFields) {
                var elementType = (ParameterizedType) listField.getGenericType();
                var elementClass = (Class<?>) elementType.getActualTypeArguments()[0];

                // ...if the child has a 1:many relationship with this model, cache it...
                if (elementClass.isAssignableFrom(this.cachedEntityClass)) {

                    this.getLogger().debug("Field named {} contains this model {} as a 1:many relationship via field {}",
                        field.getName(),
                        this.cachedEntityClass.getSimpleName(),
                        listField.getName());

                    field.setAccessible(true);
                    this.cachedEntityFieldMap.put(field, listField);
                    cached = true;
                }
            }
        }
        return cached;
    }

    /**
     * Attempt to process any 1:many and many:many relationships this model might have.
     *
     * @param field The field we're checking.
     * @return Whether or not the field was cached.
     */
    private Boolean tryCacheListFieldRelationship(final Field field) {
        var cached = false;

        // ...determine if any of this class's fields are a 1:many relationship...
        if (List.class.isAssignableFrom(field.getType())) {
            var elementType = (ParameterizedType) field.getGenericType();
            var elementClass = (Class<?>) elementType.getActualTypeArguments()[0];

            if (AbstractAclModelEntity.class.isAssignableFrom(elementClass)) {
                var subFields = Arrays.stream(FieldUtils.getAllFields(elementClass)).toList();
                var fieldReference = subFields.stream().filter(x ->
                    x.getType().isAssignableFrom(this.cachedEntityClass)).findFirst();

                // ...if we have a 1:many relationship, cache it...
                if (fieldReference.isPresent()) {
                    this.getLogger().debug("List field named {} contains this model {} as a 1:many via field {}",
                        field.getName(),
                        this.cachedEntityClass.getSimpleName(),
                        fieldReference.get().getName());

                    field.setAccessible(true);
                    fieldReference.get().setAccessible(true);
                    this.cachedEntityListFieldMap.put(field, fieldReference.get());
                    cached = true;

                    // ...otherwise check to see if the child field has a many:many relationship with this model...
                } else {
                    var listFields = subFields.stream().filter(x ->
                        List.class.isAssignableFrom(x.getType())).toList();
                    for (var listField : listFields) {
                        var childElementType = (ParameterizedType) listField.getGenericType();
                        var childElementClass = (Class<?>) childElementType.getActualTypeArguments()[0];

                        // ...if the child has a many:many relationship with this model, cache it...
                        if (childElementClass.isAssignableFrom(this.cachedEntityClass)) {

                            this.getLogger().debug("Field named {} contains this model {} as a many:many relationship via field {}",
                                field.getName(),
                                this.cachedEntityClass.getSimpleName(),
                                listField.getName());

                            field.setAccessible(true);
                            this.cachedEntityListFieldMap.put(field, listField);
                            cached = true;
                        }
                    }
                }
            }
        }
        return cached;
    }

    /**
     * Attempt to process any 1:many and many:many relationships this model might have.
     *
     * @param field The field we're checking.
     * @return Whether or not the field was cached.
     */
    private Boolean tryCacheSetFieldRelationship(final Field field) {
        var cached = false;

        // ...determine if any of this class's fields are a 1:many relationship...
        if (Set.class.isAssignableFrom(field.getType())) {
            var elementType = (ParameterizedType) field.getGenericType();
            var elementClass = (Class<?>) elementType.getActualTypeArguments()[0];

            if (AbstractAclModelEntity.class.isAssignableFrom(elementClass)) {
                var subFields = Arrays.stream(FieldUtils.getAllFields(elementClass)).toList();
                var fieldReference = subFields.stream().filter(x ->
                    x.getType().isAssignableFrom(this.cachedEntityClass)).findFirst();

                // ...if we have a 1:many relationship, cache it...
                if (fieldReference.isPresent()) {
                    this.getLogger().debug("List field named {} contains this model {} as a 1:many via field {}",
                        field.getName(),
                        this.cachedEntityClass.getSimpleName(),
                        fieldReference.get().getName());

                    field.setAccessible(true);
                    fieldReference.get().setAccessible(true);
                    this.cachedEntitySetFieldMap.put(field, fieldReference.get());
                    cached = true;

                    // ...otherwise check to see if the child field has a many:many relationship with this model...
                } else {
                    var setFields = subFields.stream().filter(x ->
                        Set.class.isAssignableFrom(x.getType())).toList();
                    for (var setField : setFields) {
                        var childElementType = (ParameterizedType) setField.getGenericType();
                        var childElementClass = (Class<?>) childElementType.getActualTypeArguments()[0];

                        // ...if the child has a many:many relationship with this model, cache it...
                        if (childElementClass.isAssignableFrom(this.cachedEntityClass)) {

                            this.getLogger().debug("Field named {} contains this model {} as a many:many relationship via field {}",
                                field.getName(),
                                this.cachedEntityClass.getSimpleName(),
                                setField.getName());

                            field.setAccessible(true);
                            this.cachedEntitySetFieldMap.put(field, setField);
                            cached = true;
                        }
                    }
                }
            }
        }
        return cached;
    }

    private String getEntityClassName(final AbstractAclModelEntity entity) {
        var name = entity.getModelType() + "Entity";
        return name;
    }
}