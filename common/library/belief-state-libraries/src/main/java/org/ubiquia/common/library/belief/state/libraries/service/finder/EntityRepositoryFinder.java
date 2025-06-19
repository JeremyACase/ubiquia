package org.ubiquia.common.library.belief.state.libraries.service.finder;


import jakarta.transaction.Transactional;
import java.util.Objects;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.belief.state.libraries.repository.EntityRepository;

/**
 * A service that can "find" a particular repository from the context.
 */
@Service
public class EntityRepositoryFinder {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Try to find a repository for the provided entity.
     *
     * @param entity The entity to find a repository for.
     * @return The entity repository if found.
     */
    @SuppressWarnings("rawtypes")
    @Transactional
    public EntityRepository findRepositoryFor(final Object entity) {
        EntityRepository entityRepository = null;
        var unproxied = Hibernate.unproxy(entity);
        entityRepository = this.findRepositoryFor(unproxied.getClass().getSimpleName());
        return entityRepository;
    }

    /**
     * Try to find a repository for the provided class name.
     *
     * @param className The class name to find a repository for.
     * @return The entity repository if found.
     */
    @SuppressWarnings("rawtypes")
    @Transactional
    public EntityRepository findRepositoryFor(final String className) {
        var beanName = className + "Repository";

        EntityRepository entityRepository = null;
        if (this.applicationContext.containsBean(beanName)) {
            entityRepository = (EntityRepository) this.applicationContext.getBean(beanName);
        } else {

            var lowerCase = Character.toLowerCase(className.charAt(0));
            beanName = lowerCase
                + className.substring(1)
                + "Repository";

            if (this.applicationContext.containsBean(beanName)) {
                entityRepository = (EntityRepository) this.applicationContext.getBean(beanName);
            }
        }

        if (Objects.isNull(entityRepository)) {
            throw new IllegalArgumentException("ERROR: Could not find repository for class: "
                + className);
        }

        return entityRepository;
    }
}