package org.ubiquia.common.library.api.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.ubiquia.common.model.ubiquia.entity.AbstractModelEntity;

/**
 * A repository interface meant to be used by anyone implementing abstract Ubiquia entities.
 *
 * @param <T> The type of the entity we're dealing with.
 */
public interface AbstractEntityRepository<T extends AbstractModelEntity>
    extends JpaRepository<T, String>,
    CrudRepository<T, String> {

    /**
     * Returns all entities of this type that have been updated more recently than their most
     * recent sync record, or that have never been synced at all.
     *
     * @return Entities that need to be synchronized with peer agents.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE "
        + "NOT EXISTS (SELECT s FROM SyncEntity s WHERE s.model = e) "
        + "OR e.updatedAt > (SELECT MAX(s.createdAt) FROM SyncEntity s WHERE s.model = e)")
    List<T> findEntitiesNeedingSync();
}
