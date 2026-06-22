package org.ubiquia.common.library.dao.service.logic;

import jakarta.persistence.EntityManager;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A service that determines whether a given class is a JPA entity type.
 */
@Service
public class EntityDeriver {

    @Autowired
    private EntityManager entityManager;

    private HashMap<Class, Boolean> cachedEntities = new HashMap<>();

    /**
     * Determine whether the provided class is registered as a JPA entity.
     *
     * @param clazz The class to check.
     * @return True if the class is an entity, false otherwise.
     */
    public boolean isEntityClass(final Class<?> clazz) {
        var isEntity = false;
        if (this.cachedEntities.containsKey(clazz)) {
            isEntity = this.cachedEntities.get(clazz);
        } else {
            isEntity = this.entityManager.getMetamodel().getEntities()
                .stream()
                .anyMatch(entityType -> entityType.getJavaType().equals(clazz));
            this.cachedEntities.put(clazz, isEntity);
        }
        return isEntity;
    }
}
