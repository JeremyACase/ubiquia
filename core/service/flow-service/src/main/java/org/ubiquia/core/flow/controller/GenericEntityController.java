package org.ubiquia.core.flow.controller;

import java.lang.reflect.ParameterizedType;
import org.springframework.beans.factory.annotation.Autowired;
import org.ubiquia.core.flow.interfaces.InterfaceLogger;
import org.ubiquia.core.flow.model.dto.AbstractEntityDto;
import org.ubiquia.core.flow.model.entity.AbstractEntity;
import org.ubiquia.core.flow.service.builder.IngressResponseBuilder;

public abstract class GenericEntityController<
    T extends AbstractEntity,
    D extends AbstractEntityDto>
    extends AbstractController
    implements InterfaceLogger {

    @Autowired
    protected IngressResponseBuilder ingressResponseBuilder;

    protected Class<T> persistedEntityClass;

    protected Class<D> persistedDtoClass;

    /**
     * Abandon all hope, ye who enter here.
     */
    @SuppressWarnings("unchecked")
    public GenericEntityController() {
        super();

        // Cache our persistent class in derived classes.
        this.persistedEntityClass = (Class<T>) ((ParameterizedType) this.getClass()
            .getGenericSuperclass()).getActualTypeArguments()[0];

        this.persistedDtoClass = (Class<D>) ((ParameterizedType) this.getClass()
            .getGenericSuperclass()).getActualTypeArguments()[1];

        this.getLogger().debug("...finished initializing controller of type {}...",
            this.persistedEntityClass.getSimpleName());
    }
}
