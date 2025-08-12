package org.ubiquia.common.library.implementation.service.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.UbiquiaAgent;
import org.ubiquia.common.model.ubiquia.entity.UbiquiaAgentEntity;

@Service
public class UbiquiaAgentDtoMapper {

    @Autowired
    private GraphDtoMapper graphDtoMapper;

    public UbiquiaAgent map(final UbiquiaAgentEntity from) throws JsonProcessingException {

        UbiquiaAgent to = null;
        if (Objects.nonNull(from)) {
            to = new UbiquiaAgent();
            to.setId(from.getId());
            to.setCreatedAt(from.getCreatedAt());
            to.setUpdatedAt(from.getUpdatedAt());
            to.setDeployedGraphs(this.graphDtoMapper.map(from.getDeployedGraphs()));
        }

        return to;
    }
}
