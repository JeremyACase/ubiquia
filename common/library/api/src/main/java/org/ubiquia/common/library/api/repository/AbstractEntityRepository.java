package org.ubiquia.common.library.api.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.ubiquia.common.model.ubiquia.entity.AbstractModelEntity;

public interface AbstractEntityRepository<T extends AbstractModelEntity>
    extends PagingAndSortingRepository<T, String>,
    CrudRepository<T, String> {

}
