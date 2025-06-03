package org.ubiquia.core.flow.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.service.mapper.GenericDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.FlowMessageDto;
import org.ubiquia.common.model.ubiquia.entity.FlowMessage;

@Service
public class FlowMessageDtoMapper extends GenericDtoMapper<FlowMessage, FlowMessageDto> {

    @Autowired
    private FlowEventDtoMapper flowEventDtoMapper;

    @Override
    public FlowMessageDto map(final FlowMessage from) throws JsonProcessingException {

        var to = new FlowMessageDto();
        super.setAbstractEntityFields(from, to);
        to.setFlowEvent(this.flowEventDtoMapper.map(from.getFlowEvent()));
        to.setPayload(from.getPayload());
        return to;
    }
}
