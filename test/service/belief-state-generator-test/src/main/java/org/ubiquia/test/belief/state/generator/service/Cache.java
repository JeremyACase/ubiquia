package org.ubiquia.test.belief.state.generator.service;

import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguage;
import org.ubiquia.test.belief.state.generator.model.Animal;
import org.ubiquia.test.belief.state.generator.model.Person;

@Service
public class Cache {

    private AgentCommunicationLanguage acl;

    private Animal animal;

    public AgentCommunicationLanguage getAcl() {
        return acl;
    }

    public void setAcl(AgentCommunicationLanguage acl) {
        this.acl = acl;
    }

    public Animal getAnimal() {
        return animal;
    }

    public void setAnimal(Animal animal) {
        this.animal = animal;
    }
}

