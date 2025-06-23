package org.ubiquia.common.library.belief.state.libraries.service.mapper;

import org.springframework.stereotype.Service;
import org.ubiquia.acl.generated.dto.Dog;
import org.ubiquia.acl.generated.entity.DogModel;

@Service
public class DogIngressDtoMapper extends AbstractIngressDtoMapper<
    Dog,
    DogModel> {

}
