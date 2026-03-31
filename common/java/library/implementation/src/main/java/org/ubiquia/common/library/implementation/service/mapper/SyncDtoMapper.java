package org.ubiquia.common.library.implementation.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Sync;
import org.ubiquia.common.model.ubiquia.entity.SyncEntity;


/**
 * A service dedicated to mapping syncs
 */
@Service
public class SyncDtoMapper {

    private AgentDtoMapper agentDtoMapper;

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

    public Sync map(final SyncEntity from) throws JsonProcessingException {

        Sync to = null;
        if (Objects.nonNull(from)) {
            to = new Sync();
            to.setId(from.getId());
            to.setModelType(from.getModelType());
            to.setCreatedAt(from.getCreatedAt());

            to.setAgent(this.agentDtoMapper.map(from.getAgent()));
        }
        return to;
    }
}
