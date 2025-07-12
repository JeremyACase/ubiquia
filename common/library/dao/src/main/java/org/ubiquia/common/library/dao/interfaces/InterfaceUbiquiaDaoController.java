package org.ubiquia.common.library.dao.interfaces;


import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.model.ubiquia.dto.AbstractModel;
import org.ubiquia.common.model.ubiquia.entity.AbstractModelEntity;

public interface InterfaceUbiquiaDaoController<
    T extends AbstractModelEntity,
    D extends AbstractModel> {

    EntityDao<T> getDataAccessObject();

    InterfaceEntityToDtoMapper<T, D> getDataTransferObjectMapper();
}
