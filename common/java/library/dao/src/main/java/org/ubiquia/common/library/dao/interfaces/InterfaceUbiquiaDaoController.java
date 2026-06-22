package org.ubiquia.common.library.dao.interfaces;


import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.model.ubiquia.dto.AbstractModel;
import org.ubiquia.common.model.ubiquia.entity.AbstractModelEntity;

/**
 * Interface defining the contract for DAO-backed RESTful controllers.
 *
 * @param <T> The entity type managed by the controller.
 * @param <D> The DTO type exposed by the controller.
 */
public interface InterfaceUbiquiaDaoController<
    T extends AbstractModelEntity,
    D extends AbstractModel> {

    /**
     * Return the data access object for the managed entity type.
     *
     * @return The EntityDao for this controller's entity.
     */
    EntityDao<T> getDataAccessObject();

    /**
     * Return the mapper that converts entities to their DTO representations.
     *
     * @return The entity-to-DTO mapper.
     */
    InterfaceEntityToDtoMapper<T, D> getDataTransferObjectMapper();
}
