package org.ubiquia.common.library.dao.repository;

import org.ubiquia.common.library.dao.model.entity.Animal;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface AnimalRepository
    extends PagingAndSortingRepository<Animal, String>,
    CrudRepository<Animal, String> {

}