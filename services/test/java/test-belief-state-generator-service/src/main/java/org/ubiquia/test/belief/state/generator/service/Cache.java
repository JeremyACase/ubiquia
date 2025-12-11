package org.ubiquia.test.belief.state.generator.service;

import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.DomainDataContract;
import org.ubiquia.test.belief.state.generator.model.Animal;
import org.ubiquia.test.belief.state.generator.model.Person;

@Service
public class Cache {

    private DomainDataContract acl;

    private Animal animal;

    private Person person;

    public DomainDataContract getAcl() {
        return acl;
    }

    public void setAcl(DomainDataContract acl) {
        this.acl = acl;
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

