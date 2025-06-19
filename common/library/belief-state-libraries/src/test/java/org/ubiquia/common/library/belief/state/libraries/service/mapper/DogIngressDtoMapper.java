package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.springframework.stereotype.Service;
import org.ubiquia.acl.generated.dto.DogDto;
import org.ubiquia.acl.generated.entity.Dog;

@Service
public class DogIngressDtoMapper extends AbstractIngressDtoMapper<
    DogDto,
    Dog> {

}
