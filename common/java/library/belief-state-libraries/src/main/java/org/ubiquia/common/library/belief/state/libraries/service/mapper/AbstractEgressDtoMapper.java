package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.library.belief.state.libraries.interfaces.service.mapper.InterfaceEgressDtoMapper;
import org.ubiquia.common.model.acl.dto.AbstractAclModel;
import org.ubiquia.common.model.acl.entity.AbstractAclModelEntity;

/**
 * An abstract class defining how DTO mappers should map ACL entities to DTO's. It will be
 * implemented by Ubiquia Belief States where models are defined in the ACL. Importantly,
 * this class WILL NOT cache the "many" side of any ACL relationships, thereby ensuring
 * that no unbounded lists get egressed to clients.
 *
 * @param <F> The entity class we're mapping from.
 * @param <T> The DTO class we're mapping to.
 */
@Service
public abstract class AbstractEgressDtoMapper<
    F extends AbstractAclModelEntity,
    T extends AbstractAclModel>
    implements InterfaceEgressDtoMapper<F, T>,
    InterfaceLogger {

    private final Class<F> cachedEntityClass;
    private final Class<T> cachedDTOClass;
    private final List<Field> cachedEntityFields = new ArrayList<>();
    private final HashMap<Field, Field> cachedDtoFieldMap = new HashMap<>();
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Constructor. Cache requisite data.
     */
    @SuppressWarnings("unchecked")
    public AbstractEgressDtoMapper() {
        this.getLogger().debug("Initializing reflection cache for mapper: {}...",
            this.getClass().getSimpleName());

        this.cachedEntityClass = this.resolveGenericClass(0);
        this.cachedDTOClass = this.resolveGenericClass(1);

        var entityFields = FieldUtils.getAllFields(this.cachedEntityClass);
        var dtoFields = FieldUtils.getAllFields(this.cachedDTOClass);

        this.matchFields(entityFields, dtoFields);

        this.cachedEntityFields.removeIf(f -> f.getName().equals("modelType"));

        this.getLogger().debug("...reflection cache initialized.");
    }

    /**
     * Map from entities to DTOs.
     *
     * @param froms The list of entities to map from.
     * @return a list of mapped DTOs.
     * @throws IllegalAccessException Exceptions from reflection.
     */
    @Transactional
    public List<AbstractAclModel> map(final List<F> froms)
        throws Exception {

        var tos = new ArrayList<AbstractAclModel>();

        if (Objects.nonNull(froms)) {
            for (var from : froms) {
                var mapped = this.map(from);
                tos.add(mapped);
            }
        }

        return tos;
    }

    /**
     * Map from an entity to a DTO.
     *
     * @param from The entity to map from.
     * @return A mapped DTO.
     * @throws IllegalAccessException Exceptions from reflection.
     */
    @Transactional
    public AbstractAclModel map(final F from)
        throws Exception {

        var to = this.getNewDto();
        to.setModelType(this.getModelType());
        to.setUbiquiaId(from.getUbiquiaId());

        this.tryHydrateObject(from, to);
        return to;
    }

    /**
     * Get the type of the generics that this class is implementing.
     *
     * @param index The element of the type being referenced.
     * @param <C>   The class type we're resolving.
     * @return The class type.
     */
    @SuppressWarnings("unchecked")
    private <C> Class<C> resolveGenericClass(final Integer index) {
        var type = (ParameterizedType) this.getClass().getGenericSuperclass();
        return (Class<C>) type.getActualTypeArguments()[index];
    }

    /**
     * Provided an entity and a mirrored DTO, cache the fields for quicker lookups later.
     *
     * @param entityFields The list of entity fields.
     * @param dtoFields    The list of DTO fields.
     */
    private void matchFields(final Field[] entityFields, final Field[] dtoFields) {
        for (var entityField : entityFields) {
            this.getLogger().debug("Processing field: {}", entityField.getName());

            var dtoMatch = Arrays.stream(dtoFields)
                .filter(dto -> dto.getName().equals(entityField.getName()))
                .findFirst();

            if (dtoMatch.isEmpty()) {
                this.getLogger().warn("No matching DTO field for: {}", entityField.getName());
            } else {
                var dtoField = dtoMatch.get();
                if (this.shouldCache(entityField)) {
                    this.cacheField(entityField, dtoField);
                } else {
                    this.getLogger().debug("Skipping field: {}", entityField.getName());
                }
            }
        }
    }

    /**
     * Determine whether field is cacheable or not.
     *
     * @param field The field to determine.
     * @return If field should be cached or not.
     */
    private Boolean shouldCache(final Field field) {
        var shouldCache = true;

        if (Modifier.isStatic(field.getModifiers())) {
            shouldCache = false;
        }

        if (isCollectionOfAclEntities(field)) {
            shouldCache = false;
        }

        return shouldCache;
    }

    /**
     * Determine if the field is a collection of Ubiquia belief state entities. This is important
     * because it would be possible to egress arbitrarily-sized collections via JSON to clients
     * unless we guard against it.
     *
     * @param field The field to check for.
     * @return Whether the field is a collection of Belief State entities or not.
     */
    private Boolean isCollectionOfAclEntities(final Field field) {

        var isCollectionOfAclEntities = true;

        if (!Collection.class.isAssignableFrom(field.getType())) {
            isCollectionOfAclEntities = false;
        } else {
            try {
                var type = (ParameterizedType) field.getGenericType();
                var arg = type.getActualTypeArguments()[0];

                if (arg instanceof Class<?> elementClass) {
                    isCollectionOfAclEntities =
                        AbstractAclModelEntity.class.isAssignableFrom(elementClass);
                }

            } catch (Exception e) {
                this.getLogger().error("Unable to inspect generic type of field: {}",
                    field.getName(),
                    e);
            }
        }
        return isCollectionOfAclEntities;
    }

    /**
     * Cache mirrored DTO and entity fields for quicker lookup later.
     *
     * @param entityField The entity field to cache.
     * @param dtoField    The DTO field to cache.
     */
    private void cacheField(final Field entityField, final Field dtoField) {
        entityField.setAccessible(true);
        dtoField.setAccessible(true);
        this.cachedEntityFields.add(entityField);
        this.cachedDtoFieldMap.put(entityField, dtoField);
        this.getLogger().debug("...cached field: {}...", entityField.getName());
    }

    /**
     * Attempt to hydrate an object per the provided egress and hydration parameters.
     *
     * @param from The entity to hydrate from.
     * @param to   The DTO to map to.
     * @throws IllegalAccessException Reflection exceptions.
     */
    @Transactional
    private void tryHydrateObject(final F from, T to)
        throws Exception {

        var unproxied = Hibernate.unproxy(from);
        if (Objects.nonNull(unproxied)) {
            for (var field : this.cachedEntityFields) {
                var dtoField = this.cachedDtoFieldMap.get(field);

                var value = field.get(unproxied);
                if (Objects.nonNull(field.get(unproxied))) {
                    if (field.isAnnotationPresent(Embedded.class) ||
                        field.getType().isAnnotationPresent(Embeddable.class)) {

                        this.tryHydrateEmbeddable(to, value, dtoField);
                    } else if (field.isAnnotationPresent(ElementCollection.class)) {
                        this.tryHydrateEmbeddables(to, value, dtoField);
                    }
                    else {
                        this.hydrateObject(to, value, dtoField);
                    }
                } else {
                    dtoField.set(to, null);
                }
            }
        }
    }

    /**
     * Attempt to hydrate embedded objects without bidirectional relationships.
     * @param to The parent object we're hydrating.
     * @param value The embedded object.
     * @param dtoField The DTO field representing the embedded object.
     * @throws Exception The usual exception clause.
     */
    private void tryHydrateEmbeddable(T to, Object value, final Field dtoField)
        throws Exception {

        var embeddedEntityClass = value.getClass();
        this.getLogger().debug("Hydrating embeddable object of class: {}",
            embeddedEntityClass);

        dtoField.setAccessible(true);
        var embeddedDtoType = dtoField.getType();
        var embeddedDto = embeddedDtoType.getDeclaredConstructor().newInstance();
        dtoField.set(to, embeddedDto);

        while (Objects.nonNull(embeddedEntityClass) && embeddedEntityClass != Object.class) {
            for (var embeddedField : embeddedEntityClass.getDeclaredFields()) {
                embeddedField.setAccessible(true);
                try {
                    var fieldValue = embeddedField.get(value);

                    var dtoEmbeddedField = embeddedDtoType
                        .getDeclaredField(embeddedField.getName());
                    dtoEmbeddedField.setAccessible(true);
                    dtoEmbeddedField.set(embeddedDto, fieldValue);

                    this.getLogger().debug("Embedded field: {} = {}",
                        embeddedField.getName(),
                        fieldValue);
                } catch (NoSuchFieldException e) {
                    // DTO doesn't have this field — skip quietly (or log at debug)
                    this.getLogger().debug("No matching DTO embedded field for '{}'",
                        embeddedField.getName());
                } catch (IllegalAccessException e) {
                    this.getLogger().warn("Unable to copy embedded field {}",
                        embeddedField.getName(),
                        e);
                }
            }
            embeddedEntityClass = embeddedEntityClass.getSuperclass();
        }
    }

    private void tryHydrateEmbeddables(T to, Object value, final Field dtoField) throws Exception {
        if (to == null || value == null || dtoField == null) {
            return;
        }

        value = Hibernate.unproxy(value);
        dtoField.setAccessible(true);

        var elementDtoType = resolveCollectionElementType(dtoField);
        var assigned = false;

        if (elementDtoType == null) {
            this.getLogger().debug("Unable to resolve collection element type for DTO field {}", dtoField.getName());
            dtoField.set(to, value);
            assigned = true;
        }

        var dest = (Collection<Object>) null;
        if (!assigned) {
            dest = newCollectionInstance(dtoField.getType());
            var iterable = asIterable(value);

            for (var raw : iterable) {
                var src = Hibernate.unproxy(raw);
                if (src != null) {
                    var dtoElem = (Object) null;
                    var alreadyAssignable = elementDtoType.isAssignableFrom(src.getClass());

                    if (alreadyAssignable) {
                        dtoElem = src;
                    } else {
                        dtoElem = elementDtoType.getDeclaredConstructor().newInstance();
                        var srcCls = src.getClass();

                        while (srcCls != null && srcCls != Object.class) {
                            var declared = srcCls.getDeclaredFields();
                            for (var sf : declared) {
                                sf.setAccessible(true);
                                Object fieldValue = null;
                                try {
                                    fieldValue = sf.get(src);
                                } catch (IllegalAccessException e) {
                                    this.getLogger().warn(
                                        "Unable to read field {} from embeddable element",
                                        sf.getName(),
                                        e
                                    );
                                }

                                try {
                                    var df = elementDtoType.getDeclaredField(sf.getName());
                                    df.setAccessible(true);
                                    df.set(dtoElem, fieldValue);
                                } catch (NoSuchFieldException ignored) {
                                    // DTO element doesn't have this field
                                }
                            }
                            srcCls = srcCls.getSuperclass();
                        }
                    }
                    dest.add(dtoElem);
                }
            }
        }

        if (!assigned) {
            dtoField.set(to, dest);
        }
    }



    /**
     * Provided an object, hydrate it with data from the database.
     *
     * @param to       The object to hydrate.
     * @param value    The value to fetch from teh database.
     * @param dtoField The mirrored DTO field to use to set our values.
     * @throws IllegalAccessException Exceptions from Reflection.
     */
    private void hydrateObject(T to, Object value, final Field dtoField)
        throws Exception {

        value = Hibernate.unproxy(value);

        if (List.class.isAssignableFrom(value.getClass())) {
            var values = new ArrayList<>();
            for (var element : (List<?>) value) {
                var unproxied = Hibernate.unproxy(element);
                if (Objects.nonNull(unproxied)) {
                    values.add(unproxied);
                }
            }
            dtoField.set(to, values);
        } else if (Set.class.isAssignableFrom(value.getClass())) {
            var values = new HashSet<>();
            for (var element : (Set<?>) value) {
                var unproxied = Hibernate.unproxy(element);
                if (Objects.nonNull(unproxied)) {
                    values.add(unproxied);
                }
            }
            dtoField.set(to, values);
        } else {
            this.trySetDtoField(to, dtoField, value);
        }
    }

    /**
     * Attempt to set the DTO and its field with the values from the database.
     *
     * @param dto      The DTO to hydrate.
     * @param dtoField The specific field to set.
     * @param value    The value we're updating the field with.
     * @throws IllegalAccessException Reflection exceptions.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Transactional
    private void trySetDtoField(T dto, final Field dtoField, final Object value)
        throws Exception {

        var unproxied = Hibernate.unproxy(value);
        if (Objects.nonNull(unproxied)) {

            var type = unproxied.getClass();

            this.getLogger().debug("Attempting to map field name '{}' of type: '{}'...",
                dtoField.getName(),
                type.getSimpleName());

            if (AbstractAclModelEntity.class.isAssignableFrom(type)) {
                var lowerCase = Character.toLowerCase(type.getSimpleName().charAt(0));
                var simpleName = lowerCase + type.getSimpleName().substring(1);

                var fullTypeCamelCase = simpleName + "EgressDtoMapper";
                fullTypeCamelCase = fullTypeCamelCase.replace("Entity", "");

                var fullType = type.getSimpleName() + "EgressDtoMapper";
                fullType = fullTypeCamelCase.replace("Entity", "");

                if (this.applicationContext.containsBean(fullTypeCamelCase)) {
                    this.getLogger().debug("...Found mapper bean for type {}; attempting to map...",
                        fullTypeCamelCase);
                    var mapperBean = (AbstractEgressDtoMapper)
                        this.applicationContext
                            .getBean(fullTypeCamelCase);

                    var mapped = mapperBean.map((AbstractAclModelEntity) unproxied);
                    mapped = (AbstractAclModel) Hibernate.unproxy(mapped);
                    dtoField.set(dto, mapped);

                } else if (this.applicationContext.containsBean(fullType)) {

                    this.getLogger().debug("...Found mapper bean for type {}; attempting to map...",
                        fullType);

                    var mapperBean = (AbstractEgressDtoMapper)
                        this.applicationContext
                            .getBean(fullType);

                    var mapped = mapperBean.map((AbstractAclModelEntity) unproxied);
                    mapped = (AbstractAclModel) Hibernate.unproxy(mapped);
                    dtoField.set(dto, mapped);
                }
            } else {
                this.getLogger().debug("...Type '{}' is not a Ubiquia entity; "
                        + "assigning value as-is...",
                    type);
                this.getLogger().debug("Field and Value: {} : {}", dtoField.getName(), value);

                dtoField.set(dto, value);
            }
        }
    }

    private Class<?> resolveCollectionElementType(final Field field) {
        var result = (Class<?>) null;
        var g = field.getGenericType();

        if (g instanceof ParameterizedType pt) {
            var args = pt.getActualTypeArguments();
            if (Objects.nonNull(args) && args.length == 1) {
                var a = args[0];
                if (a instanceof Class<?> c) {
                    result = c;
                } else if (a instanceof ParameterizedType p
                    && p.getRawType() instanceof Class<?> rc) {
                    result = rc;
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> newCollectionInstance(final Class<?> collectionType) {
        var result = (Collection<Object>) null;

        var canConstructDirect =
            collectionType != null &&
                !collectionType.isInterface() &&
                !Modifier.isAbstract(collectionType.getModifiers());

        if (canConstructDirect) {
            try {
                result = (Collection<Object>) collectionType
                    .getDeclaredConstructor()
                    .newInstance();
            } catch (Exception ignored) {
                // fall through to defaults
            }
        }

        if (Objects.isNull(result)) {
            if (Set.class.isAssignableFrom(collectionType)) {
                result = new HashSet<>();
            } else {
                result = new ArrayList<>();
            }
        }
        return result;
    }

    private Iterable<?> asIterable(final Object value) {
        var list = new ArrayList<>();

        if (value instanceof Iterable<?> it) {
            for (var o : it) {
                list.add(o);
            }

        } else if (Objects.nonNull(value) && value.getClass().isArray()) {
            var len = java.lang.reflect.Array.getLength(value);
            for (var i = 0; i < len; i++) {
                list.add(java.lang.reflect.Array.get(value, i));
            }
        } else if (Objects.nonNull(value)) {
            list.add(value);
        }
        return list;
    }
}
