package org.ubiquia.common.library.implementation.service.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.DomainDataContract;
import org.ubiquia.common.model.ubiquia.entity.DomainDataContractEntity;


@Service
public class DomainDataContractDtoMapper extends GenericDtoMapper<
    DomainDataContractEntity,
    DomainDataContract> {

    @Override
    public DomainDataContract map(final DomainDataContractEntity from)
        throws JsonProcessingException {

        DomainDataContract to = null;
        if (Objects.nonNull(from)) {
            to = new DomainDataContract();

            super.setAbstractEntityFields(from, to);

            to.setJsonSchema(from.getJsonSchema());
        }
        return to;
    }
}
