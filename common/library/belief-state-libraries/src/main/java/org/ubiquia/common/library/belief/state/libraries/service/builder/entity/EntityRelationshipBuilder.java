package org.ubiquia.common.library.belief.state.libraries.service.builder.entity;

import jakarta.transaction.Transactional;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Supplier;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.library.belief.state.libraries.service.finder.EntityRelationshipBuilderFinder;
import org.ubiquia.common.library.belief.state.libraries.service.finder.EntityRepositoryFinder;
import org.ubiquia.common.model.acl.entity.AbstractAclModelEntity;

@Service
public abstract class EntityRelationshipBuilder<T extends AbstractAclModelEntity> implements InterfaceLogger {

    protected final Map<Field, Field> cachedEntityFieldMap = new HashMap<>();
    protected final Map<Field, Field> cachedEntityListFieldMap = new HashMap<>();
    protected final Map<Field, Field> cachedEntitySetFieldMap = new HashMap<>();
    protected final Class<T> cachedEntityClass;

    @Autowired
    private EntityRelationshipBuilderFinder entityRelationshipBuilderFinder;

    @Autowired
    private EntityRepositoryFinder repositoryFinder;

    @SuppressWarnings("unchecked")
    public EntityRelationshipBuilder() {
        this.getLogger().info("Caching reflection data...");
        this.cachedEntityClass = (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        var fields = FieldUtils.getAllFields(this.cachedEntityClass);

        for (var field : fields) {
            if (!this.tryCacheFieldRelationship(field)) {
                if (!this.tryCacheListFieldRelationship(field)) {
                    this.tryCacheSetFieldRelationship(field);
                }
            }
        }
        this.getLogger().info("...finished caching reflection data.");
    }

    public void tryBuildRelationships(T model) throws Exception {
        var entityName = getEntityClassName(model);
        var builder = (EntityRelationshipBuilder<T>) tryGetRelationshipBuilder(model);
        if (!this.cachedEntityClass.getName().equalsIgnoreCase(entityName) && Objects.nonNull(builder)) {
            builder.tryBuildRelationships(model);
        } else {
            this.trySetRelationships(model);
        }
    }

    private EntityRelationshipBuilder<?> tryGetRelationshipBuilder(AbstractAclModelEntity model) {
        var entityName = getEntityClassName(model);
        EntityRelationshipBuilder<?> builder = null;
        if (!entityName.equalsIgnoreCase(this.cachedEntityClass.getSimpleName())) {
            builder = entityRelationshipBuilderFinder.findRelationshipBuilderFor(model);
        }
        return builder;
    }

    @Transactional
    private void trySetRelationships(T model) throws Exception {
        for (var key : cachedEntityFieldMap.keySet()) {
            handleBidirectionalRelationship(model, key, key.get(model), cachedEntityFieldMap.get(key));
        }

        for (var key : cachedEntityListFieldMap.keySet()) {
            var values = (List<?>) key.get(model);
            key.set(model, new ArrayList<>()); // reset to collect updated objects
            if (values != null) {
                for (var value : values) {
                    handleBidirectionalRelationship(model, key, value, cachedEntityListFieldMap.get(key));
                }
            }
        }

        for (var key : cachedEntitySetFieldMap.keySet()) {
            var values = (Set<?>) key.get(model);
            key.set(model, new HashSet<>());
            if (values != null) {
                for (var value : values) {
                    handleBidirectionalRelationship(model, key, value, cachedEntitySetFieldMap.get(key));
                }
            }
        }
    }

    private void handleBidirectionalRelationship(
        T parent,
        Field parentField,
        Object child,
        Field childBackRefField)
        throws Exception {

        if (Objects.nonNull(child)) {
            var repository = repositoryFinder.findRepositoryFor(child);
            var idOpt = getIdValue(child);

            Object persistedChild;
            if (idOpt.isPresent()) {
                Object fetched = null;
                try {
                    fetched = repository.findById(idOpt.get()).orElseThrow(() -> new IllegalArgumentException("No entity with ID: " + idOpt.get()));
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                linkParentToChild(fetched, parent, childBackRefField);
                persistedChild = repository.save(fetched);
            } else {
                tryBuildNestedRelationshipsIfNeeded(child);
                linkParentToChild(child, parent, childBackRefField);
                persistedChild = repository.save(child);
            }

            updateParentFieldWithChild(parent, parentField, persistedChild);
        }
    }

    private void linkParentToChild(Object child, Object parent, Field childBackRefField) throws Exception {
        if (List.class.isAssignableFrom(childBackRefField.getType())) {
            var list = ensureCollectionInitialized(childBackRefField, child, ArrayList::new);
            list.add(parent);
        } else if (Set.class.isAssignableFrom(childBackRefField.getType())) {
            var set = ensureCollectionInitialized(childBackRefField, child, HashSet::new);
            set.add(parent);
        } else {
            childBackRefField.set(child, parent);
        }
    }

    private void updateParentFieldWithChild(Object parent, Field parentField, Object child) throws Exception {
        if (List.class.isAssignableFrom(parentField.getType())) {
            var list = ensureCollectionInitialized(parentField, parent, ArrayList::new);
            list.add(child);
        } else if (Set.class.isAssignableFrom(parentField.getType())) {
            var set = ensureCollectionInitialized(parentField, parent, HashSet::new);
            set.add(child);
        } else {
            parentField.set(parent, child);
        }
    }

    private void tryBuildNestedRelationshipsIfNeeded(Object child) throws Exception {
        if ((child instanceof AbstractAclModelEntity entity)) {
            var entityName = getEntityClassName(entity);
            if (!entityName.equalsIgnoreCase(this.cachedEntityClass.getSimpleName())) {
                var builder = (EntityRelationshipBuilder<T>) tryGetRelationshipBuilder(entity);
                if (Objects.nonNull(builder)) {
                    builder.tryBuildRelationships((T) entity);
                }
            }
        }
    }

    private Optional<Object> getIdValue(Object entity) {
        var fields = FieldUtils.getAllFields(entity.getClass());
        var fieldsList = Arrays.stream(fields).toList();
        var id = fieldsList
            .stream()
            .filter(x -> x.getName().equals("id"))
            .findFirst()
            .map(x -> {
                x.setAccessible(true);
                try {
                    return x.get(entity);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Unable to access ID field: " + e);
                }
            });
        return id;
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> ensureCollectionInitialized(Field field, Object entity, Supplier<? extends Collection<Object>> factory) throws IllegalAccessException {
        field.setAccessible(true);
        Collection<Object> collection = (Collection<Object>) field.get(entity);
        if (Objects.isNull(collection)) {
            collection = factory.get();
            field.set(entity, collection);
        }
        return collection;
    }

    private boolean tryCacheFieldRelationship(Field field) {
        var cached = false;
        if (AbstractAclModelEntity.class.isAssignableFrom(field.getType())) {
            cached = tryCacheEntityRelationship(field);
        }
        return cached;
    }

    private boolean tryCacheEntityRelationship(Field field) {
        var cached = false;

        var subFields = Arrays.asList(FieldUtils.getAllFields(field.getType()));
        var directRef = subFields.stream().filter(f -> f.getType().isAssignableFrom(cachedEntityClass)).findFirst();
        if (directRef.isPresent()) {
            field.setAccessible(true);
            directRef.get().setAccessible(true);
            cachedEntityFieldMap.put(field, directRef.get());
            cached = true;
        }
        var listField = subFields.stream().filter(f -> List.class.isAssignableFrom(f.getType())).findFirst();
        if (listField.isPresent()) {
            var paramType = (ParameterizedType) listField.get().getGenericType();
            var elementClass = (Class<?>) paramType.getActualTypeArguments()[0];
            if (elementClass.isAssignableFrom(cachedEntityClass)) {
                field.setAccessible(true);
                cachedEntityFieldMap.put(field, listField.get());
                cached = true;
            }
        }
        return cached;
    }

    private boolean tryCacheListFieldRelationship(Field field) {
        return tryCacheCollectionField(field, List.class, cachedEntityListFieldMap);
    }

    private boolean tryCacheSetFieldRelationship(Field field) {
        return tryCacheCollectionField(field, Set.class, cachedEntitySetFieldMap);
    }

    private boolean tryCacheCollectionField(Field field, Class<?> collectionClass, Map<Field, Field> cache) {
        var cached = false;

        if ((field.getGenericType() instanceof ParameterizedType parameterizedType)) {
            var elementClass = (Class<?>) parameterizedType.getActualTypeArguments()[0];

            var subFields = Arrays.asList(FieldUtils.getAllFields(elementClass));
            var backRef = subFields.stream().filter(f -> f.getType().isAssignableFrom(cachedEntityClass)).findFirst();
            if (backRef.isPresent()) {
                field.setAccessible(true);
                backRef.get().setAccessible(true);
                cache.put(field, backRef.get());
                cached = true;
            }

            var sameCollectionFields = subFields.stream().filter(f -> collectionClass.isAssignableFrom(f.getType()));
            for (var f : sameCollectionFields.toList()) {
                var childElementType = (ParameterizedType) f.getGenericType();
                var childClass = (Class<?>) childElementType.getActualTypeArguments()[0];
                if (childClass.isAssignableFrom(cachedEntityClass)) {
                    field.setAccessible(true);
                    cache.put(field, f);
                    cached = true;
                }
            }
        }
        
        return cached;
    }

    private String getEntityClassName(final AbstractAclModelEntity entity) {
        return entity.getModelType() + "Entity";
    }
}
