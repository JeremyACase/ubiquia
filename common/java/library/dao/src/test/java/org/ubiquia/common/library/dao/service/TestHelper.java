package org.ubiquia.common.library.dao.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.dao.repository.AnimalRepository;
import org.ubiquia.common.library.dao.repository.PersonRepository;

/** Helper service to reset database state between integration tests. */
@Service
public class TestHelper {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private AnimalRepository animalRepository;

    /**
     * Delete all animal and person records from the database.
     */
    public void clearState() {
        this.animalRepository.deleteAll();
        this.personRepository.deleteAll();
    }
}
