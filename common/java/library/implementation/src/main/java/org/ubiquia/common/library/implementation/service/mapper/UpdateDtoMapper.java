package org.ubiquia.common.library.implementation.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Agent;
import org.ubiquia.common.model.ubiquia.dto.Update;
import org.ubiquia.common.model.ubiquia.entity.UpdateEntity;


/**
 * A service dedicated to mapping updates
 */
@Service
public class UpdateDtoMapper {

    @Autowired
    private AgentDtoMapper agentDtoMapper;

    public List<Update> map(final List<UpdateEntity> froms) throws JsonProcessingException {
        var tos = new ArrayList<Update>();
        if (Objects.nonNull(froms)) {
            for (var from : froms) {
                var to = this.map(from);
                tos.add(to);
            }
        }
        return tos;
    }

    public List<Update> map(final Set<UpdateEntity> froms) throws JsonProcessingException {
        var tos = new ArrayList<Update>();
        if (Objects.nonNull(froms)) {
            for (var from : froms) {
                var to = this.map(from);
                tos.add(to);
            }
        }
        return tos;
    }

    public Update map(final UpdateEntity from) throws JsonProcessingException {

        Update to = null;
        if (Objects.nonNull(from)) {
            to = new Update();
            to.setId(from.getId());
            to.setModelType(from.getModelType());
            to.setCreatedAt(from.getCreatedAt());
            to.setUpdateReason(from.getUpdateReason());

            to.setAgent(this.agentDtoMapper.map(from.getAgent()));

        }
        return to;
    }

}
