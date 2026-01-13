package org.ubiquia.common.library.belief.state.libraries.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.ubiquia.common.model.domain.entity.AbstractDomainModelEntity;

/**
 * An interface defining methods for models for generated belief states.
 *
 * @param <T> The generated entity type.
 */
public interface EntityRepository<T extends AbstractDomainModelEntity>
    extends PagingAndSortingRepository<T, String>,
    CrudRepository<T, String> {

    /**
     * Find all distinct tag keys for this model.
     *
     * @return A list of distinct tag keys.
     */
    @Query("SELECT DISTINCT et.key FROM #{#entityName} e JOIN e.ubiquiaTags et")
    List<String> findAllDistinctTagKeys();

    /**
     * Find all distinct tag values for this model.
     *
     * @return A list of distinct tag values.
     */
    @Query("SELECT DISTINCT et.value FROM #{#entityName} e JOIN e.ubiquiaTags et")
    List<String> findAllDistinctTagValues();

    /**
     * A list of distinct tag values provided a key.
     *
     * @param key The key to look for distinct tag values.
     * @return The list of unique values.
     */
    @Query("SELECT DISTINCT et.value FROM #{#entityName} e JOIN e.ubiquiaTags et WHERE et.key = ?1")
    List<String> findAllDistinctTagValuesByKey(String key);
}
