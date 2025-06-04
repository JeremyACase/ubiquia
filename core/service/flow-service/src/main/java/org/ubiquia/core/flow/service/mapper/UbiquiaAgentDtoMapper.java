package org.ubiquia.core.flow.service.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.api.service.mapper.GenericDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.GraphDto;
import org.ubiquia.common.model.ubiquia.dto.UbiquiaAgentDto;
import org.ubiquia.common.model.ubiquia.embeddable.NameAndVersionPair;
import org.ubiquia.common.model.ubiquia.entity.Graph;
import org.ubiquia.common.model.ubiquia.entity.UbiquiaAgent;

@Service
public class UbiquiaAgentDtoMapper {

    @Autowired
    private GraphDtoMapper graphDtoMapper;

    public UbiquiaAgentDto map(final UbiquiaAgent from) throws JsonProcessingException {

        UbiquiaAgentDto to = null;
        if (Objects.nonNull(from)) {
            to = new UbiquiaAgentDto();
            to.setId(from.getId());
            to.setCreatedAt(from.getCreatedAt());
            to.setUpdatedAt(from.getUpdatedAt());
            to.setDeployedGraphs(this.graphDtoMapper.map(from.getDeployedGraphs()));
        }

        return to;
    }
}
