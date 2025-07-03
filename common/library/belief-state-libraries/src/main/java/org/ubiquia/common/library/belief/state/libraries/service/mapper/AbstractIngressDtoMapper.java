package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.belief.state.libraries.service.finder.EntityRepositoryFinder;
import org.ubiquia.common.model.acl.dto.AbstractAclModel;
import org.ubiquia.common.model.acl.entity.AbstractAclModelEntity;

@Service
public abstract class AbstractIngressDtoMapper<
    F extends AbstractAclModel,
    T extends AbstractAclModelEntity> {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private EntityRepositoryFinder entityRepositoryFinder;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Translate from an ingress Data Transfer Object to a MACHINA model.
     *
     * @param from The DTO we're translating from.
     * @return The translated, typed class.
     */
    public T map(final F from, Class<T> toClass)
        throws ClassNotFoundException,
        IllegalAccessException {

        var fromMapped = this.objectMapper.convertValue(from, Map.class);
        var toMapped = new HashMap<>();
        for (var key : fromMapped.keySet()) {
            if (Objects.nonNull(fromMapped.get(key))) {
                toMapped.put(key, fromMapped.get(key));
            }
        }
        var to = this.objectMapper.convertValue(toMapped, toClass);
        this.tryHydrateRelationships(to);
        return to;
    }

    /**
     * This method will use database relationships to associate entities during ingress in
     * order to avoid any potential overwriting issues.
     *
     * @param to The model we're converting from a DTO to an entity.
     * @throws ClassNotFoundException Exception from not being able to find a MACHINA class.
     * @throws IllegalAccessException Reflection exceptions.
     */
    @Transactional
    private void tryHydrateRelationships(T to)
        throws ClassNotFoundException,
        IllegalAccessException {

        if (AbstractAclModelEntity.class.isAssignableFrom(to.getClass())) {

            var cast = (AbstractAclModelEntity) to;

            var clazz = Class.forName("org.ubiquia.acl.generated."
                + cast.getModelType()
                + "Entity");

            for (var field : FieldUtils.getAllFields(clazz)) {
                field.setAccessible(true);

                // Only bother to hydrate our relationship if the value isn't null....
                if (Objects.nonNull(field.get(to))) {

                    // ...if we have a single Ubiquia entity...
                    if (AbstractAclModelEntity.class.isAssignableFrom(field.getType())) {
                        this.tryHydrateOneToOneRelationship(to, field);

                    // ...else if we have a list of Ubiquia entities...
                    } else if (List.class.isAssignableFrom(field.getType())) {
                        this.tryHydrateOneToManyRelationship(to, field);
                    }
                }
            }
        }
    }

    /**
     * Attempt to hydrate a field representing a 1:many database relationships.
     *
     * @param to    The object we're hydrating.
     * @param field The object's field we're hydrating.
     * @throws IllegalAccessException Reflection exceptions.
     */
    @SuppressWarnings("unchecked")
    private void tryHydrateOneToManyRelationship(T to, final Field field)
        throws IllegalAccessException {

        var elementType = (ParameterizedType) field.getGenericType();
        var elementClass = (Class<?>) elementType.getActualTypeArguments()[0];

        if (AbstractAclModelEntity.class.isAssignableFrom(elementClass)
            && Objects.nonNull(field.get(to))) {

            var hydratedList = new ArrayList<AbstractAclModelEntity>();
            for (var element : (List<?>) field.get(to)) {

                var repository = this
                    .entityRepositoryFinder
                    .findRepositoryFor(element);

                var entity = (AbstractAclModelEntity) element;

                // ...use any provided ID's to create our database relationships.
                if (Objects.nonNull(entity.getId())) {
                    var record = repository.findById(entity.getId());
                    if (record.isEmpty()) {
                        throw new IllegalArgumentException("ERROR: Entity not found: "
                            + entity.getId());
                    }
                    var hydrated = (AbstractAclModelEntity) Hibernate.unproxy(record.get());
                    hydratedList.add(hydrated);
                } else {
                    var hydrated = (AbstractAclModelEntity) Hibernate.unproxy(element);
                    hydratedList.add(hydrated);
                }
            }
            field.set(to, hydratedList);
        }
    }

    /**
     * Attempt to hydrate an object's field representing a 1:1 database relationship.
     *
     * @param to The object we're hydrating.
     * @param field The object's field we're hydrating.
     * @throws IllegalAccessException Reflection exceptions.
     */
    @SuppressWarnings("unchecked")
    private void tryHydrateOneToOneRelationship(T to, final Field field)
        throws IllegalAccessException {

        var entity = (AbstractAclModelEntity) field.get(to);

        // we have been provided an ID, use it to create to associate our entities.
        if (Objects.nonNull(entity.getId())) {
            var repository = this
                .entityRepositoryFinder
                .findRepositoryFor(entity);

            var record = repository.findById(entity.getId());
            if (record.isEmpty()) {
                throw new IllegalArgumentException("ERROR: Entity not found: "
                    + entity.getId());
            }

            var hydrated = Hibernate.unproxy(record.get());
            field.set(to, hydrated);
        }
    }
}