package org.ubiquia.common.library.implementation.service.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.entity.DomainOntologyEntity;
import org.ubiquia.common.model.ubiquia.entity.NodeEntity;


@Service
public class DomainOntologyDtoMapper extends GenericDtoMapper<
    DomainOntologyEntity,
    DomainOntology> {

    @Autowired
    private GraphDtoMapper graphDtoMapper;

    @Autowired
    private DomainDataContractDtoMapper domainDataContractDtoMapper;

    @Override
    public DomainOntology map(final DomainOntologyEntity from) throws JsonProcessingException {

        DomainOntology to = null;
        if (Objects.nonNull(from)) {
            to = new DomainOntology();

            super.setAbstractEntityFields(from, to);

            to.setAuthor(from.getAuthor());
            to.setDescription(from.getDescription());
            to.setName(from.getName());
            to.setGraphs(this.graphDtoMapper.map(from.getGraphs()));
            to.setVersion(from.getVersion());
            to.setDomainDataContract(this
                .domainDataContractDtoMapper
                .map(from.getDomainDataContract()));
        }
        return to;
    }
}
