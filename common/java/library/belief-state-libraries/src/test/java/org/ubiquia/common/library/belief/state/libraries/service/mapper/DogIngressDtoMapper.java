package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.springframework.stereotype.Service;
import org.ubiquia.acl.generated.Dog;
import org.ubiquia.acl.generated.DogEntity;

@Service
public class DogIngressDtoMapper extends AbstractIngressDtoMapper<
    Dog,
    DogEntity> {

}
