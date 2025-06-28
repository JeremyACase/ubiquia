package org.ubiquia.common.library.api.interfaces;


import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.model.ubiquia.dto.AbstractModel;

public interface InterfaceUbiquiaDaoController<
    T extends org.ubiquia.common.model.ubiquia.entity.AbstractModelEntity,
    D extends AbstractModel> {

    EntityDao<T> getDataAccessObject();

    InterfaceEntityToDtoMapper<T, D> getDataTransferObjectMapper();
}
