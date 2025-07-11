package org.ubiquia.common.library.belief.state.libraries.service.factory;

import static org.instancio.Select.field;

import java.util.ArrayList;
import org.instancio.Instancio;
import org.springframework.stereotype.Service;
import org.ubiquia.acl.generated.*;
import org.ubiquia.common.model.acl.dto.AbstractAclModel;

@Service
public class MockFactory {

    /**
     * Generate a mock model for testing.
     *
     * @return A Mock model.
     */
    public Person generatePerson() {
        var model = Instancio
            .of(Person.class)
            .ignore(field(AbstractAclModel::getId))
            .ignore(field(AbstractAclModel::getCreatedAt))
            .ignore(field(AbstractAclModel::getUpdatedAt))
            .set(field(Person::getPets), new ArrayList<>())
            .set(field(AbstractAclModel::getModelType), "Person")
            .create();
        return model;
    }

    /**
     * Generate a mock model for testing.
     *
     * @return A mock model.
     */
    public Cat generateCat() {
        var model = Instancio
            .of(Cat.class)
            .ignore(field(AbstractAclModel::getId))
            .ignore(field(AbstractAclModel::getCreatedAt))
            .ignore(field(AbstractAclModel::getUpdatedAt))
            .ignore(field(Animal::getOwner))
            .create();
        return model;
    }

    /**
     * Generate a mock model for testing.
     *
     * @return A mock model.
     */
    public Shark generateShark() {
        var model = Instancio
            .of(Shark.class)
            .ignore(field(AbstractAclModel::getId))
            .ignore(field(AbstractAclModel::getCreatedAt))
            .ignore(field(AbstractAclModel::getUpdatedAt))
            .ignore(field(Animal::getOwner))
            .create();
        return model;
    }

    /**
     * Generate a mock model for testing.
     *
     * @return A mock model.
     */
    public Dachschund generateWienerDog() {
        var warWeen = Instancio
            .of(Dachschund.class)
            .ignore(field(AbstractAclModel::getId))
            .ignore(field(AbstractAclModel::getCreatedAt))
            .ignore(field(AbstractAclModel::getUpdatedAt))
            .ignore(field(Animal::getOwner))
            .create();
        return warWeen;
    }

}