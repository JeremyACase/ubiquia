package org.ubiquia.common.library.implementation.service.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.DomainDataContract;
import org.ubiquia.common.model.ubiquia.entity.DomainDataContractEntity;

/** Mapper service for converting {@link DomainDataContractEntity} to {@link DomainDataContract}. */
@Service
public class AgentCommunicationLanguageDtoMapper extends GenericDtoMapper<
    DomainDataContractEntity,
    DomainDataContract> {

    @Autowired
    private GraphDtoMapper graphDtoMapper;

    @Override
    public DomainDataContract map(final DomainDataContractEntity from)
        throws JsonProcessingException {

        DomainDataContract to = null;
        if (Objects.nonNull(from)) {
            to = new DomainDataContract();
            super.setAbstractEntityFields(from, to);
            to.setSchema(super.objectMapper.readValue(from.getSchema(), Object.class));
        }
        return to;
    }
}
