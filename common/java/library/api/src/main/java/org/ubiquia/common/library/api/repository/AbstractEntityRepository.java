package org.ubiquia.common.library.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.ubiquia.common.model.ubiquia.entity.AbstractModelEntity;

/**
 * A repository interface meant to be used by anyone implementing abstract Ubiquia entities.
 *
 * @param <T> The type of the entity we're dealing with.
 */
public interface AbstractEntityRepository<T extends AbstractModelEntity>
    extends JpaRepository<T, String>,
    CrudRepository<T, String> {

}
