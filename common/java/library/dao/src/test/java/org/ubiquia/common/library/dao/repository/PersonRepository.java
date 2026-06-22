package org.ubiquia.common.library.dao.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.ubiquia.common.library.dao.model.entity.Person;

/** Spring Data repository for {@link Person} test entities. */
public interface PersonRepository
    extends PagingAndSortingRepository<Person, String>,
    CrudRepository<Person, String> {

}