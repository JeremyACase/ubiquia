package org.ubiquia.common.library.implementation.service.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.DomainDataContract;
import org.ubiquia.common.model.ubiquia.entity.DomainDataContractEntity;

@Service
public class AgentCommunicationLanguageDtoMapper extends GenericDtoMapper<
    DomainDataContractEntity,
    DomainDataContract> {

    @Autowired
    private GraphDtoMapper graphDTOMapper;

    @Override
    public DomainDataContract map(final DomainDataContractEntity from)
        throws JsonProcessingException {

        DomainDataContract to = null;
        if (Objects.nonNull(from)) {
            to = new DomainDataContract();
            super.setAbstractEntityFields(from, to);
            to.setJsonSchema(super.objectMapper.readValue(from.getJsonSchema(), Object.class));
        }
        return to;
    }
}
