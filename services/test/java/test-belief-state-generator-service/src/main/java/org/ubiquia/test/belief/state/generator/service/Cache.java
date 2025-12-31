package org.ubiquia.test.belief.state.generator.service;

import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.test.belief.state.generator.model.Animal;
import org.ubiquia.test.belief.state.generator.model.Person;

@Service
public class Cache {

    private DomainOntology domainOntology;

    private Animal animal;

    private Person person;

    public DomainOntology getDomainOntology() {
        return domainOntology;
    }

    public void setDomainOntology(DomainOntology domainOntology) {
        this.domainOntology = domainOntology;
    }

    public Animal getAnimal() {
        return animal;
    }

    public void setAnimal(Animal animal) {
        this.animal = animal;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }
}

