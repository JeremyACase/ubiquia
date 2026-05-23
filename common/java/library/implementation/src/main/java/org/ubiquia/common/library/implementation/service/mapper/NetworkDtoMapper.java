package org.ubiquia.common.library.implementation.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Network;
import org.ubiquia.common.model.ubiquia.entity.NetworkEntity;

@Service
public class NetworkDtoMapper extends GenericDtoMapper<NetworkEntity, Network> {

    @Override
    public Network map(final NetworkEntity from) throws JsonProcessingException {
        Network to = null;
        if (Objects.nonNull(from)) {
            to = new Network();
            super.setAbstractEntityFields(from, to);
        }
        return to;
    }
}
