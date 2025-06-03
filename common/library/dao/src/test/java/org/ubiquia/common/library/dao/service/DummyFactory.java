package org.ubiquia.common.library.dao.service;


import static org.instancio.Select.field;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.instancio.Instancio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.dao.model.entity.*;
import org.ubiquia.common.library.dao.repository.AnimalRepository;
import org.ubiquia.common.library.dao.repository.PersonRepository;
import org.ubiquia.common.model.ubiquia.embeddable.KeyValuePair;

@Service
public class DummyFactory {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private AnimalRepository animalRepository;

    /**
     * Generate a person entity with pets for the purpose of testing our library.
     */
    @Transactional
    public Person generatePersonWithPets() {

        var pets = new ArrayList<Animal>();
        pets.add(this.generateCat());
        pets.add(this.generateWienerDog());
        pets.add(this.generatePoodle());

        var person = Instancio
            .of(Person.class)
            .withMaxDepth(1)
            .ignore(field(Person::getId))
            .ignore(field(Person::getCreatedAt))
            .ignore(field(Person::getUpdatedAt))
            .set(field(Person::getPets), pets)
            .set(field(Person::getNullValue), null)
            .create();

        for (var pet : pets) {
            pet.setOwner(person);
        }

        person = this.personRepository.save(person);
        this.animalRepository.saveAll(pets);

        return person;

    }

    /**
     * Generate a list of tags for this factory.
     *
     * @return A list of tags.
     */
    public List<KeyValuePair> generateListTags() {

        var tags = new ArrayList<KeyValuePair>();
        for (int i = 0; i < 3; i++) {
            var tag = Instancio.of(KeyValuePair.class)
                .withMaxDepth(1)
                .create();
            tags.add(tag);
        }
        return tags;

    }

    /**
     * Generate a set of tags for this factory.
     *
     * @return A set of tags.
     */
    public Set<KeyValuePair> generateSetTags() {

        var tags = new HashSet<KeyValuePair>();
        for (int i = 0; i < 3; i++) {
            var tag = Instancio.of(KeyValuePair.class)
                .withMaxDepth(1)
                .create();
            tags.add(tag);
        }
        return tags;

    }

    /**
     * Generate a cat entity.
     *
     * @return A cat for testing.
     */
    public Cat generateCat() {
        var cat = Instancio
            .of(Cat.class)
            .ignore(field(Animal::getId))
            .ignore(field(Animal::getCreatedAt))
            .ignore(field(Animal::getUpdatedAt))
            .set(field(Animal::getListTags), this.generateListTags())
            .set(field(Animal::getSetTags), this.generateSetTags())
            .withMaxDepth(1)
            .create();

        return cat;

    }

    /**
     * Generate a dog entity.
     *
     * @return A dog for testing.
     */
    public Dog generateDog() {
        var dog = Instancio
            .of(Dog.class)
            .ignore(field(Animal::getId))
            .ignore(field(Animal::getCreatedAt))
            .ignore(field(Animal::getUpdatedAt))
            .set(field(Animal::getListTags), this.generateListTags())
            .set(field(Animal::getSetTags), this.generateSetTags())
            .withMaxDepth(1)
            .create();

        return dog;
    }

    /**
     * Generate the most majestic of all creatures, in this world or any other world.
     *
     * @return An apex predator.
     */
    public Dog generateWienerDog() {
        var dog = Instancio
            .of(Dachschund.class)
            .ignore(field(Animal::getId))
            .ignore(field(Animal::getCreatedAt))
            .ignore(field(Animal::getUpdatedAt))
            .ignore(field(Dachschund::getApexPredator)) // No need to generate, it's already known
            .ignore(field(Dachschund::getLandShark))
            .set(field(Animal::getListTags), this.generateListTags())
            .set(field(Animal::getSetTags), this.generateSetTags())
            .withMaxDepth(1)
            .create();

        return dog;
    }

    /**
     * Generate a budget wiener dog.
     *
     * @return A poodle.
     */
    public Dog generatePoodle() {
        var dog = Instancio
            .of(Poodle.class)
            .ignore(field(Animal::getId))
            .ignore(field(Animal::getCreatedAt))
            .ignore(field(Animal::getUpdatedAt))
            .ignore(field(Poodle::getApexPredator))
            .set(field(Animal::getListTags), this.generateListTags())
            .set(field(Animal::getSetTags), this.generateSetTags())
            .withMaxDepth(1)
            .create();

        return dog;
    }
}
