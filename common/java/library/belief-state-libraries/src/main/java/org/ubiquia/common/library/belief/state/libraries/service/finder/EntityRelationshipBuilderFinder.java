package org.ubiquia.common.library.belief.state.libraries.service.finder;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.EntityRelationshipBuilder;
import org.ubiquia.common.model.acl.entity.AbstractAclModelEntity;

/**
 * A service that can "find" a particular entity relationship builder from the context.
 */
@Service
public class EntityRelationshipBuilderFinder {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Given an entity, attempt to get retrieve a bean that knows how to map its relationships.
     *
     * @param entity The entity to retrieve a mapper for.
     * @return The mapper if found, else null.
     */
    @SuppressWarnings("rawtypes")
    public EntityRelationshipBuilder findRelationshipBuilderFor(
        final AbstractAclModelEntity entity) {

        EntityRelationshipBuilder mapperBean = null;

        var mapperBeanName = entity.getModelType() + "RelationshipBuilder";
        if (this.applicationContext.containsBean(mapperBeanName)) {
            mapperBean = (EntityRelationshipBuilder)
                this.applicationContext.getBean(mapperBeanName);
        } else {
            var lowerCase = Character.toLowerCase(entity.getModelType().charAt(0));
            mapperBeanName = lowerCase
                + entity.getModelType().substring(1)
                + "RelationshipBuilder";
            if (this.applicationContext.containsBean(mapperBeanName)) {
                mapperBean = (EntityRelationshipBuilder)
                    this.applicationContext.getBean(mapperBeanName);
            }
        }
        return mapperBean;
    }
}