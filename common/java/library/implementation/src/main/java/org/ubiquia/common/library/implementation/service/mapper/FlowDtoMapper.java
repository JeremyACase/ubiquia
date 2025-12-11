package org.ubiquia.common.library.implementation.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Flow;
import org.ubiquia.common.model.ubiquia.entity.FlowEntity;


@Service
public class FlowDtoMapper extends GenericDtoMapper<FlowEntity, Flow> {

    private static final Logger logger = LoggerFactory.getLogger(FlowDtoMapper.class);

    @Autowired
    private GraphDtoMapper graphDtoMapper;

    @Override
    public Flow map(final FlowEntity from) throws JsonProcessingException {

        Flow to = null;
        if (Objects.nonNull(from)) {
            to = new Flow();
            super.setAbstractEntityFields(from, to);
            to.setGraph(this.graphDtoMapper.map(from.getGraph()));
        }
        return to;
    }
}
