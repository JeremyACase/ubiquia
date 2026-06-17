package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.springframework.stereotype.Service;
import org.ubiquia.domain.generated.Person;
import org.ubiquia.domain.generated.PersonEntity;

/** Ingress mapper for person domain models. */
@Service
public class PersonIngressDtoMapper extends AbstractIngressDtoMapper<
    Person,
    PersonEntity> {

}
