package org.ubiquia.common.library.belief.state.libraries.service.finder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.belief.state.libraries.service.mapper.AbstractEgressDtoMapper;
import org.ubiquia.common.model.acl.entity.AbstractAclModelEntity;

/**
 * A service that can "find" a particular egress mapper from the context.
 */
@Service
public class EgressMapperFinder {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Provided a Ubiquia ACL entity, "Find" the appropriate mapper from the context.
     *
     * @param entity
     * @return The appropriate mapper.
     */
    @SuppressWarnings("rawtypes")
    public AbstractEgressDtoMapper findEgressMapperFor(final AbstractAclModelEntity entity) {
        var beanName = this.tryGetMapperBeanNameCamelCased(entity);
        AbstractEgressDtoMapper mapperBean = null;
        if (this.applicationContext.containsBean(beanName)) {
            mapperBean = (AbstractEgressDtoMapper) this.applicationContext.getBean(beanName);
        } else {
            beanName = this.getMapperBeanNameSimple(entity);
            mapperBean = (AbstractEgressDtoMapper) this.applicationContext.getBean(beanName);
        }
        return mapperBean;
    }

    /**
     * Attempt to get a mapper bean from Spring context for camel cased beans.
     *
     * @param entity The entity to build a mapper bean name for.
     * @return The mapper bean name.
     */
    private String tryGetMapperBeanNameCamelCased(final AbstractAclModelEntity entity) {
        var lowerCase = Character.toLowerCase(entity.getModelType().charAt(0));
        var simpleName = lowerCase + entity.getModelType().substring(1);
        var beanName = simpleName + "EgressDtoMapper";
        return beanName;
    }

    /**
     * Attempt to get a mapper bean from Spring context for camel cased beans.
     *
     * @param entity The entity to build a mapper bean name for.
     * @return The mapper bean name.
     */
    private String getMapperBeanNameSimple(final AbstractAclModelEntity entity) {
        var beanName = entity.getModelType() + "EgressDtoMapper";
        return beanName;
    }
}
