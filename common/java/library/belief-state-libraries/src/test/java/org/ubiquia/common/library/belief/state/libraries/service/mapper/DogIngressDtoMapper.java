package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.springframework.stereotype.Service;
import org.ubiquia.domain.generated.Dog;
import org.ubiquia.domain.generated.DogEntity;

/** Ingress mapper for dog domain models. */
@Service
public class DogIngressDtoMapper extends AbstractIngressDtoMapper<
    Dog,
    DogEntity> {

}
