package org.ubiquia.common.library.implementation.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Agent;
import org.ubiquia.common.model.ubiquia.dto.Sync;
import org.ubiquia.common.model.ubiquia.entity.SyncEntity;


/**
 * A service dedicated to mapping syncs.
 */
@Service
public class SyncDtoMapper {

    /**
     * Map a list of sync entities to DTOs.
     *
     * @param froms The entities to map.
     * @return A list of mapped {@link Sync} DTOs.
     * @throws JsonProcessingException If payload deserialization fails.
     */
    public List<Sync> map(final List<SyncEntity> froms) throws JsonProcessingException {
        var tos = new ArrayList<Sync>();
        if (Objects.nonNull(froms)) {
            for (var from : froms) {
                var to = this.map(from);
                tos.add(to);
            }
        }
        return tos;
    }

    /**
     * Map a set of sync entities to DTOs.
     *
     * @param froms The entities to map.
     * @return A list of mapped {@link Sync} DTOs.
     * @throws JsonProcessingException If payload deserialization fails.
     */
    public List<Sync> map(final Set<SyncEntity> froms) throws JsonProcessingException {
        var tos = new ArrayList<Sync>();
        if (Objects.nonNull(froms)) {
            for (var from : froms) {
                var to = this.map(from);
                tos.add(to);
            }
        }
        return tos;
    }

    /**
     * Map a single sync entity to its DTO representation.
     *
     * @param from The entity to map from.
     * @return The mapped {@link Sync} DTO, or null if the input is null.
     * @throws JsonProcessingException If payload deserialization fails.
     */
    public Sync map(final SyncEntity from) throws JsonProcessingException {

        Sync to = null;
        if (Objects.nonNull(from)) {
            to = new Sync();
            to.setId(from.getId());
            to.setModelType(from.getModelType());
            to.setCreatedAt(from.getCreatedAt());

            var agent = new Agent();
            agent.setId(from.getSourceAgent().getId());
            to.setSourceAgent(agent);
        }
        return to;
    }
}
