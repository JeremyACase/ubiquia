package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.springframework.stereotype.Service;
import org.ubiquia.acl.generated.dto.PersonDto;
import org.ubiquia.acl.generated.entity.Person;

@Service
public class PersonIngressDtoMapper extends AbstractIngressDtoMapper<
    PersonDto,
    Person> {

}
