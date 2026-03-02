package org.ubiquia.common.library.implementation.service.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Agent;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;

@Service
public class AgentDtoMapper {

    @Autowired
    private GraphDtoMapper graphDtoMapper;

    public Agent map(final AgentEntity from) throws JsonProcessingException {

        Agent to = null;
        if (Objects.nonNull(from)) {
            to = new Agent();
            to.setId(from.getId());
            to.setCreatedAt(from.getCreatedAt());
            to.setUpdatedAt(from.getUpdatedAt());
            to.setDeployedGraphs(this.graphDtoMapper.map(from.getDeployedGraphs()));
        }

        return to;
    }
}
