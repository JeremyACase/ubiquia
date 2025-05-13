package org.ubiquia.core.flow.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.ubiquia.core.flow.model.entity.AbstractEntity;

public interface AbstractEntityRepository<T extends AbstractEntity>
    extends PagingAndSortingRepository<T, String>,
    CrudRepository<T, String> {

}
