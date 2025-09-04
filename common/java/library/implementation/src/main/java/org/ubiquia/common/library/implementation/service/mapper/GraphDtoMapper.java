package org.ubiquia.common.library.implementation.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.model.ubiquia.embeddable.NameAndVersionPair;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;

@Service
public class GraphDtoMapper extends GenericDtoMapper<GraphEntity, Graph> {

    @Autowired
    private AdapterDtoMapper adapterDtoMapper;

    @Autowired
    private ComponentDtoMapper componentDtoMapper;

    @Override
    public Graph map(final GraphEntity from) throws JsonProcessingException {

        Graph to = null;
        if (Objects.nonNull(from)) {
            to = new Graph();
            super.setAbstractEntityFields(from, to);

            to.setAuthor(from.getAuthor());
            to.setCapabilities(from.getCapabilities());
            to.setDescription(from.getDescription());
            to.setName(from.getName());
            to.setVersion(from.getVersion());

            to.setAgentCommunicationLanguage(new NameAndVersionPair());
            to.getAgentCommunicationLanguage().setName(from.getAgentCommunicationLanguage().getDomain());
            to.getAgentCommunicationLanguage().setVersion(from.getAgentCommunicationLanguage().getVersion());

            to.setComponents(this.componentDtoMapper.map(from.getComponents()));
            to.setAdapters(this.adapterDtoMapper.map(from.getAdapters()));
        }

        return to;
    }
}
