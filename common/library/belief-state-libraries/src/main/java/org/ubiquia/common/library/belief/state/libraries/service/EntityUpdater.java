package org.ubiquia.common.library.belief.state.libraries.service;


import java.lang.reflect.Field;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.acl.embeddable.KeyValuePair;
import org.ubiquia.common.model.acl.entity.AbstractAclModelEntity;

/**
 * A service dedicated to updating fields for entity models.
 */
@Service
public class EntityUpdater {

    protected static final Logger logger = LoggerFactory.getLogger(EntityUpdater.class);

    /**
     * Given a list of key-value-pairs representing field keys and values to update,
     * attempt to update an entity with the appropriate typing.
     *
     * @param entity        The entity to update.
     * @param keyValuePairs The key value pairs to use towards updating.
     * @throws IllegalAccessException Reflection exceptions.
     */
    public void trySetValue(
        AbstractAclModelEntity entity,
        final List<KeyValuePair> keyValuePairs) throws IllegalAccessException {

        logger.debug("Attempting to set entity of type {} with id {} with key-value-pair {}",
            entity.getClass().getSimpleName(),
            entity.getUbiquiaId(),
            keyValuePairs);

        var fields = FieldUtils.getAllFieldsList(entity.getClass());
        for (var kvp : keyValuePairs) {
            var fieldMatch = fields.stream().filter(x ->
                    x.getName().equalsIgnoreCase(kvp.getKey()))
                .findFirst();
            if (fieldMatch.isEmpty()) {
                throw new IllegalArgumentException("Could not find field named: " + kvp.getKey());
            }
            var field = fieldMatch.get();
            field.setAccessible(true);
            this.trySetValueViaType(entity, field, kvp);
        }
    }

    /**
     * Try to set a specific field provided an update value.
     *
     * @param field        The field to update.
     * @param keyValuePair The key-value-pair to use towards updating.
     * @param entity       The entity to update.
     * @throws IllegalAccessException Reflection exceptions.
     */
    private void trySetValueViaType(
        AbstractAclModelEntity entity,
        final Field field,
        final KeyValuePair keyValuePair) throws IllegalAccessException {

        var value = keyValuePair.getValue();

        if (Objects.isNull(value) || value.equalsIgnoreCase("null")) {
            field.set(entity, null);
        } else if (field.getType().equals(Integer.class)) {
            field.set(entity, Integer.valueOf(value));
        } else if (field.getType().equals(Float.class)) {
            field.set(entity, Float.valueOf(value));
        } else if (field.getType().equals(Double.class)) {
            field.set(entity, Double.valueOf(value));
        } else if (field.getType().equals(Boolean.class)) {
            field.set(entity, Boolean.valueOf(value));
        } else if (field.getType().equals(Date.class)) {
            field.set(entity, Date.from(Instant.parse(value)));
        } else if (field.getType().equals(OffsetDateTime.class)) {
            field.set(entity, OffsetDateTime.parse(value));
        } else if (field.getType().isEnum()) {
            // Java enums are capitalized
            Class clazz = field.getType();
            var enumValue = Enum.valueOf(clazz, value.toUpperCase());
            field.set(entity, enumValue);
        } else if (field.getType().equals(String.class)) {
            field.set(entity, value);
        } else if (List.class.isAssignableFrom(field.getType())) {
            logger.debug("Class derived as a list; adding value of key-value-pair as "
                + "an element...");
            var list = (List<Object>) field.get(entity);
            if (Objects.isNull(list)) {
                list = new ArrayList<>();
                field.set(entity, list);
            }
            list.add(keyValuePair.getValue());
        } else if (AbstractAclModelEntity.class.isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException("ERROR: Cannot update an entire AEntity entity; "
                + "; maybe try updating the child entity instead?");
        }
    }
}
