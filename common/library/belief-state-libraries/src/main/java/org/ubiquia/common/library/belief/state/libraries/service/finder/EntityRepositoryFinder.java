package org.ubiquia.common.library.belief.state.libraries.service.finder;


import jakarta.transaction.Transactional;
import java.util.Objects;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.library.belief.state.libraries.repository.EntityRepository;

/**
 * A service that can "find" a particular repository from the context.
 */
@Service
@Transactional
public class EntityRepositoryFinder implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(EntityRepositoryFinder.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public Logger getLogger() {
        return logger;
    }

    /**
     * Try to find a repository for the provided entity.
     *
     * @param entity The entity to find a repository for.
     * @return The entity repository if found.
     */
    @SuppressWarnings("rawtypes")
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
    public EntityRepository findRepositoryFor(final String className) {

        var entityName = className.replace("Entity", "");
        var beanName = entityName + "Repository";

        this.getLogger().debug("...finding repository for bean name: {}",
            beanName);

        EntityRepository entityRepository = null;
        if (this.applicationContext.containsBean(beanName)) {
            entityRepository = (EntityRepository) this.applicationContext.getBean(beanName);
        } else {

            var lowerCase = Character.toLowerCase(entityName.charAt(0));
            beanName = lowerCase
                + entityName.substring(1)
                + "Repository";

            this.getLogger().debug("...could not find original bean name, attempting lower "
                    + "case: {}",
                beanName);

            if (this.applicationContext.containsBean(beanName)) {
                entityRepository = (EntityRepository) this.applicationContext.getBean(beanName);
            }
        }

        if (Objects.isNull(entityRepository)) {
            this.getLogger().error("Could not find repository for bean name: {}", beanName);
            throw new IllegalArgumentException("ERROR: Could not find repository for class: "
                + className);
        }

        this.getLogger().debug("...found repository: {}",
            entityRepository.getClass().getSimpleName());

        return entityRepository;
    }
}