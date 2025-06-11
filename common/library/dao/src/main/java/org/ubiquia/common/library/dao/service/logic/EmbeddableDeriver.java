package org.ubiquia.common.library.dao.service.logic;

import jakarta.persistence.EntityManager;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmbeddableDeriver {

    @Autowired
    private EntityManager entityManager;

    private HashMap<Class, Boolean> cachedEntities = new HashMap<>();

    public boolean isEmbeddedClass(final Class<?> clazz) {
        var isEntity = false;
        if (this.cachedEntities.containsKey(clazz)) {
            isEntity = this.cachedEntities.get(clazz);
        } else {
            isEntity = this.entityManager.getMetamodel().getEmbeddables()
                .stream()
                .anyMatch(entityType -> entityType.getJavaType().equals(clazz));
            this.cachedEntities.put(clazz, isEntity);
        }
        return isEntity;
    }
}
