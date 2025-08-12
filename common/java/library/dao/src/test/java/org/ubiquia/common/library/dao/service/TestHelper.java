package org.ubiquia.common.library.dao.service;

import org.ubiquia.common.library.dao.repository.AnimalRepository;
import org.ubiquia.common.library.dao.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestHelper {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private AnimalRepository animalRepository;

    public void clearState() {
        this.animalRepository.deleteAll();
        this.personRepository.deleteAll();
    }
}
