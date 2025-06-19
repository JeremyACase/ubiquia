package org.ubiquia.common.library.belief.state.libraries.service.finder;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.belief.state.libraries.service.mapper.AbstractIngressDtoMapper;
import org.ubiquia.common.model.acl.dto.AbstractAclEntityDto;

@Service
public class IngressMapperFinder {

    @Autowired
    private ApplicationContext applicationContext;

    @SuppressWarnings("rawtypes")
    public AbstractIngressDtoMapper findEgressMapperFor(final AbstractAclEntityDto model) {
        var beanName = this.tryGetMapperBeanNameCamelCased(model);
        AbstractIngressDtoMapper mapperBean = null;
        if (this.applicationContext.containsBean(beanName)) {
            mapperBean = (AbstractIngressDtoMapper) this.applicationContext.getBean(beanName);
        } else {
            beanName = this.getMapperBeanNameSimple(model);
            mapperBean = (AbstractIngressDtoMapper) this.applicationContext.getBean(beanName);
        }
        return mapperBean;
    }


    private String tryGetMapperBeanNameCamelCased(final AbstractAclEntityDto model) {
        var lowerCase = Character.toLowerCase(model.getClass().getSimpleName().charAt(0));
        var simpleName = lowerCase + model.getClass().getSimpleName().substring(1);
        simpleName = simpleName.replace("Dto", "");
        var beanName = simpleName + "IngressDtoMapper";
        return beanName;
    }

    private String getMapperBeanNameSimple(final AbstractAclEntityDto model) {
        var simpleName = model.getClass().getSimpleName();
        simpleName = simpleName.replace("Dto", "");
        var beanName = simpleName + "IngressDtoMapper";
        return beanName;
    }
}
