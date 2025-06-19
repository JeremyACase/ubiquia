package org.ubiquia.common.library.belief.state.libraries.service.factory;

import static org.instancio.Select.field;

import java.util.ArrayList;
import org.instancio.Instancio;
import org.springframework.stereotype.Service;
import org.ubiquia.acl.generated.dto.*;
import org.ubiquia.common.model.acl.dto.AbstractAclEntityDto;

@Service
public class MockFactory {

    /**
     * Generate a mock model for testing.
     *
     * @return A Mock model.
     */
    public PersonDto generatePerson() {
        var model = Instancio
            .of(PersonDto.class)
            .ignore(field(AbstractAclEntityDto::getId))
            .ignore(field(AbstractAclEntityDto::getCreatedAt))
            .ignore(field(AbstractAclEntityDto::getUpdatedAt))
            .set(field(PersonDto::getPets), new ArrayList<>())
            .set(field(AbstractAclEntityDto::getModelType), "Person")
            .create();
        return model;
    }

    /**
     * Generate a mock model for testing.
     *
     * @return A mock model.
     */
    public CatDto generateCat() {
        var model = Instancio
            .of(CatDto.class)
            .ignore(field(AbstractAclEntityDto::getId))
            .ignore(field(AbstractAclEntityDto::getCreatedAt))
            .ignore(field(AbstractAclEntityDto::getUpdatedAt))
            .ignore(field(AnimalDto::getOwner))
            .create();
        return model;
    }

    /**
     * Generate a mock model for testing.
     *
     * @return A mock model.
     */
    public SharkDto generateShark() {
        var model = Instancio
            .of(SharkDto.class)
            .ignore(field(AbstractAclEntityDto::getId))
            .ignore(field(AbstractAclEntityDto::getCreatedAt))
            .ignore(field(AbstractAclEntityDto::getUpdatedAt))
            .ignore(field(AnimalDto::getOwner))
            .create();
        return model;
    }

    /**
     * Generate a mock model for testing.
     *
     * @return A mock model.
     */
    public DachschundDto generateWienerDog() {
        var warWeen = Instancio
            .of(DachschundDto.class)
            .ignore(field(AbstractAclEntityDto::getId))
            .ignore(field(AbstractAclEntityDto::getCreatedAt))
            .ignore(field(AbstractAclEntityDto::getUpdatedAt))
            .ignore(field(AnimalDto::getOwner))
            .create();
        return warWeen;
    }

}