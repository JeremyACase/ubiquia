package org.ubiquia.common.library.dao.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.ubiquia.common.library.dao.model.entity.Animal;

/** Spring Data repository for {@link Animal} test entities. */
public interface AnimalRepository
    extends PagingAndSortingRepository<Animal, String>,
    CrudRepository<Animal, String> {

}