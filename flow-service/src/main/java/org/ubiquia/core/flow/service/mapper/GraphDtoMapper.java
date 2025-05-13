package org.ubiquia.core.flow.service.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.model.dto.GraphDto;
import org.ubiquia.core.flow.model.embeddable.NameAndVersionPair;
import org.ubiquia.core.flow.model.entity.Graph;

@Service
public class GraphDtoMapper extends GenericDtoMapper<Graph, GraphDto> {

    @Autowired
    private AdapterDtoMapper adapterDtoMapper;

    @Autowired
    private AgentDtoMapper agentDtoMapper;

    @Override
    public GraphDto map(final Graph from) throws JsonProcessingException {

        GraphDto to = null;
        if (Objects.nonNull(from)) {
            to = new GraphDto();
            super.setAbstractEntityFields(from, to);

            to.setAuthor(from.getAuthor());
            to.setCapabilities(from.getCapabilities());
            to.setDescription(from.getDescription());
            to.setGraphName(from.getGraphName());
            to.setVersion(from.getVersion());

            to.setAgentCommunicationLanguage(new NameAndVersionPair());
            to.getAgentCommunicationLanguage().setName(from.getAgentCommunicationLanguage().getDomain());
            to.getAgentCommunicationLanguage().setVersion(from.getAgentCommunicationLanguage().getVersion());

            to.setAgents(this.agentDtoMapper.map(from.getAgents()));
            to.setAdapters(this.adapterDtoMapper.map(from.getAdapters()));
        }

        return to;
    }
}
