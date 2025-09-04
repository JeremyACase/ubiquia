package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.springframework.stereotype.Service;
import org.ubiquia.acl.generated.Person;
import org.ubiquia.acl.generated.PersonEntity;

@Service
public class PersonIngressDtoMapper extends AbstractIngressDtoMapper<
    Person,
    PersonEntity> {

}
