package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.springframework.stereotype.Service;
import org.ubiquia.acl.generated.dto.Person;
import org.ubiquia.acl.generated.entity.PersonModel;

@Service
public class PersonIngressDtoMapper extends AbstractIngressDtoMapper<
    Person,
    PersonModel> {

}
