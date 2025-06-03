package org.ubiquia.common.library.api.interfaces;


import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.model.ubiquia.dto.AbstractEntityDto;
import org.ubiquia.common.model.ubiquia.entity.AbstractEntity;

public interface InterfaceUbiquiaDaoController<
    T extends AbstractEntity,
    D extends AbstractEntityDto> {

    EntityDao<T> getDataAccessObject();

    InterfaceEntityToDtoMapper<T, D> getDataTransferObjectMapper();
}
