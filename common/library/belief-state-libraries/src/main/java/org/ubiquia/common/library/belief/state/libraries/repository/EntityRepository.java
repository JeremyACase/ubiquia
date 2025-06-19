package org.ubiquia.common.library.belief.state.libraries.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.ubiquia.common.model.acl.entity.AbstractAclEntity;

public interface EntityRepository<T extends AbstractAclEntity>
    extends PagingAndSortingRepository<T, String>,
    CrudRepository<T, String> {

    @Query("SELECT DISTINCT et.key FROM #{#entityName} e JOIN e.tags et")
    List<String> findAllDistinctTagKeys();

    @Query("SELECT DISTINCT et.value FROM #{#entityName} e JOIN e.tags et")
    List<String> findAllDistinctTagValues();

    @Query("SELECT DISTINCT et.value FROM #{#entityName} e JOIN e.tags et WHERE et.key = ?1")
    List<String> findAllDistinctTagValuesByKey(String key);
}
