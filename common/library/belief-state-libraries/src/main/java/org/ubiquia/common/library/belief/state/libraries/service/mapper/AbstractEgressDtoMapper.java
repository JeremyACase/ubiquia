package org.ubiquia.common.library.belief.state.libraries.service.mapper;

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
import org.ubiquia.common.model.acl.dto.AbstractAclEntityDto;
import org.ubiquia.common.model.acl.entity.AbstractAclEntity;

@Service
public abstract class AbstractEgressDtoMapper<
    F extends AbstractAclEntity,
    T extends AbstractAclEntityDto>
    implements InterfaceEgressDtoMapper<F, T>,
    InterfaceLogger {

    private final Class<F> cachedEntityClass;
    private final Class<T> cachedDTOClass;
    private final List<Field> cachedEntityFields = new ArrayList<>();
    private final HashMap<Field, Field> cachedDtoFieldMap = new HashMap<>();
    @Autowired
    private ApplicationContext applicationContext;

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

    @Transactional
    public List<AbstractAclEntityDto> map(List<F> froms)
        throws IllegalAccessException {

        var tos = new ArrayList<AbstractAclEntityDto>();

        if (Objects.nonNull(froms)) {
            for (var from : froms) {
                var mapped = this.map(from);
                tos.add(mapped);
            }
        }

        return tos;
    }

    @Transactional
    public AbstractAclEntityDto map(F from)
        throws IllegalAccessException {

        var to = this.getNewDto();
        to.setModelType(this.getModelType());
        to.setId(from.getId());

        this.tryHydrateObject(from, to);
        return to;
    }

    @SuppressWarnings("unchecked")
    private <C> Class<C> resolveGenericClass(int index) {
        var type = (ParameterizedType) this.getClass().getGenericSuperclass();
        return (Class<C>) type.getActualTypeArguments()[index];
    }

    private void matchFields(final Field[] entityFields, final Field[] dtoFields) {
        for (var entityField : entityFields) {
            this.getLogger().debug("Processing field: {}", entityField.getName());

            var dtoMatch = Arrays.stream(dtoFields)
                .filter(dto -> dto.getName().equals(entityField.getName()))
                .findFirst();

            if (dtoMatch.isEmpty()) {
                this.getLogger().warn("No matching DTO field for: {}", entityField.getName());
                continue;
            }

            var dtoField = dtoMatch.get();

            if (this.shouldCache(entityField)) {
                this.cacheField(entityField, dtoField);
            } else {
                this.getLogger().debug("Skipping field: {}", entityField.getName());
            }
        }
    }

    private boolean shouldCache(Field field) {
        var shouldCache = true;
        if (Modifier.isStatic(field.getModifiers())) {
            shouldCache = false;
        }

        if (isCollectionOfAclEntities(field)) {
            shouldCache = false;
        }

        return shouldCache;
    }

    private boolean isCollectionOfAclEntities(Field field) {

        var isCollectionOfAclEntities = true;

        if (!Collection.class.isAssignableFrom(field.getType())) {
            isCollectionOfAclEntities = false;
        }

        try {
            var type = (ParameterizedType) field.getGenericType();
            var arg = type.getActualTypeArguments()[0];

            if (arg instanceof Class<?> elementClass) {
                isCollectionOfAclEntities = AbstractAclEntity.class.isAssignableFrom(elementClass);
            }

        } catch (Exception e) {
            this.getLogger().warn("Unable to inspect generic type of field: {}",
                field.getName(),
                e);
        }

        return isCollectionOfAclEntities;
    }

    private void cacheField(Field entityField, Field dtoField) {
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
    private void tryHydrateObject(F from, T to)
        throws IllegalAccessException {

        var unproxied = Hibernate.unproxy(from);
        if (Objects.nonNull(unproxied)) {
            for (var field : this.cachedEntityFields) {
                var dtoField = this.cachedDtoFieldMap.get(field);

                var value = field.get(unproxied);
                if (Objects.nonNull(field.get(unproxied))) {
                    this.hydrateObject(to, value, dtoField);
                } else {
                    dtoField.set(to, null);
                }
            }
        }
    }

    private void hydrateObject(T to, Object value, final Field dtoField)
        throws IllegalAccessException {

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

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Transactional
    private void trySetDtoField(T dto, final Field dtoField, final Object value)
        throws IllegalAccessException {

        var unproxied = Hibernate.unproxy(value);
        if (Objects.nonNull(unproxied)) {

            var type = unproxied.getClass();

            this.getLogger().debug("Attempting to map field name '{}' of type: '{}'...",
                dtoField.getName(),
                type.getSimpleName());

            if (AbstractAclEntity.class.isAssignableFrom(type)) {
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

                    var mapped = mapperBean.map((AbstractAclEntity) unproxied);
                    mapped = (AbstractAclEntityDto) Hibernate.unproxy(mapped);
                    dtoField.set(dto, mapped);

                } else if (applicationContext.containsBean(fullType)) {

                    this.getLogger().debug("...Found mapper bean for type {}; attempting to map...",
                        fullType);

                    var mapperBean = (AbstractEgressDtoMapper)
                        this.applicationContext
                            .getBean(fullType);

                    var mapped = mapperBean.map((AbstractAclEntity) unproxied);
                    mapped = (AbstractAclEntityDto) Hibernate.unproxy(mapped);
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
}
