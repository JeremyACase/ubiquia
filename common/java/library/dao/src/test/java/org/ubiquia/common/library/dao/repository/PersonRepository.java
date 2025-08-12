package org.ubiquia.common.library.dao.repository;

import org.ubiquia.common.library.dao.model.entity.Person;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface PersonRepository
    extends PagingAndSortingRepository<Person, String>,
    CrudRepository<Person, String> {

}