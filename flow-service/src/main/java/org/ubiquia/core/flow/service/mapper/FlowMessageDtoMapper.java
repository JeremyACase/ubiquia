package org.ubiquia.core.flow.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.model.dto.FlowMessageDto;
import org.ubiquia.core.flow.model.entity.FlowMessage;

@Service
public class FlowMessageDtoMapper extends GenericDtoMapper<FlowMessage, FlowMessageDto> {

    @Autowired
    private FlowEventDtoMapper flowEventDTOMapper;

    @Override
    public FlowMessageDto map(final FlowMessage from) throws JsonProcessingException {

        var to = new FlowMessageDto();
        super.setAEntityFields(from, to);
        to.setFlowEvent(this.flowEventDTOMapper.map(from.getAmigosEvent()));
        to.setPayload(from.getPayload());
        return to;
    }
}
