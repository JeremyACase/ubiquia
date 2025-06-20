package org.ubiquia.common.library.belief.state.libraries.service.finder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.belief.state.libraries.service.mapper.AbstractEgressDtoMapper;
import org.ubiquia.common.model.acl.entity.AbstractAclEntity;

/**
 * A service that can "find" a particular egress mapper from the context.
 *
 */
@Service
public class EgressMapperFinder {

    @Autowired
    private ApplicationContext applicationContext;

    @SuppressWarnings("rawtypes")
    public AbstractEgressDtoMapper findEgressMapperFor(final AbstractAclEntity model) {
        var beanName = this.tryGetMapperBeanNameCamelCased(model);
        AbstractEgressDtoMapper mapperBean = null;
        if (this.applicationContext.containsBean(beanName)) {
            mapperBean = (AbstractEgressDtoMapper) this.applicationContext.getBean(beanName);
        } else {
            beanName = this.getMapperBeanNameSimple(model);
            mapperBean = (AbstractEgressDtoMapper) this.applicationContext.getBean(beanName);
        }
        return mapperBean;
    }


    private String tryGetMapperBeanNameCamelCased(final AbstractAclEntity model) {
        var lowerCase = Character.toLowerCase(model.getModelType().charAt(0));
        var simpleName = lowerCase + model.getModelType().substring(1);
        var beanName = simpleName + "EgressDtoMapper";
        return beanName;
    }

    private String getMapperBeanNameSimple(final AbstractAclEntity model) {
        var beanName = model.getModelType() + "EgressDtoMapper";
        return beanName;
    }
}
