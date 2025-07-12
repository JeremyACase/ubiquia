package org.ubiquia.common.library.implementation.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.common.model.ubiquia.entity.FlowMessageEntity;

@Service
public class FlowMessageDtoMapper extends GenericDtoMapper<FlowMessageEntity, FlowMessage> {

    @Autowired
    private FlowEventDtoMapper flowEventDtoMapper;

    @Override
    public FlowMessage map(final FlowMessageEntity from) throws JsonProcessingException {

        var to = new FlowMessage();
        super.setAbstractEntityFields(from, to);
        to.setFlowEvent(this.flowEventDtoMapper.map(from.getFlowEvent()));
        to.setPayload(from.getPayload());
        return to;
    }
}
