package org.ubiquia.common.library.implementation.service.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Agent;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;

/** Mapper service for converting {@link AgentEntity} to {@link Agent} DTOs. */
@Service
public class AgentDtoMapper {

    @Autowired
    private GraphDtoMapper graphDtoMapper;

    /**
     * Map an agent entity to its DTO representation.
     *
     * @param from The entity to map from.
     * @return The mapped {@link Agent} DTO, or null if the input is null.
     * @throws JsonProcessingException If payload deserialization fails.
     */
    public Agent map(final AgentEntity from) throws JsonProcessingException {

        Agent to = null;
        if (Objects.nonNull(from)) {
            to = new Agent();
            to.setId(from.getId());
            to.setCreatedAt(from.getCreatedAt());
            to.setUpdatedAt(from.getUpdatedAt());
            to.setBaseUrl(from.getBaseUrl());
            to.setReachable(from.isReachable());
            to.setDeployedGraphs(this.graphDtoMapper.map(from.getDeployedGraphs()));
        }

        return to;
    }
}
