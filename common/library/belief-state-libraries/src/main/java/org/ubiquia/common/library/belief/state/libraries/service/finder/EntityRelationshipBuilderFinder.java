package org.ubiquia.common.library.belief.state.libraries.service.finder;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.EntityRelationshipBuilder;
import org.ubiquia.common.model.acl.entity.AbstractAclEntity;

/**
 * A service that can "find" a particular entity relationship builder from the context.
 */
@Service
public class EntityRelationshipBuilderFinder {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Given a model, attempt to get retrieve a bean that knows how to map its relationships.
     *
     * @param model The model to retrieve a mapper for.
     * @return The mapper if found, else null.
     */
    @SuppressWarnings("rawtypes")
    public EntityRelationshipBuilder findRelationshipBuilderFor(final AbstractAclEntity model) {
        EntityRelationshipBuilder mapperBean = null;

        var mapperBeanName = model.getModelType() + "RelationshipBuilder";
        if (this.applicationContext.containsBean(mapperBeanName)) {
            mapperBean = (EntityRelationshipBuilder)
                this.applicationContext.getBean(mapperBeanName);
        } else {
            var lowerCase = Character.toLowerCase(model.getModelType().charAt(0));
            mapperBeanName = lowerCase
                + model.getModelType().substring(1)
                + "RelationshipBuilder";
            if (this.applicationContext.containsBean(mapperBeanName)) {
                mapperBean = (EntityRelationshipBuilder)
                    this.applicationContext.getBean(mapperBeanName);
            }
        }
        return mapperBean;
    }
}