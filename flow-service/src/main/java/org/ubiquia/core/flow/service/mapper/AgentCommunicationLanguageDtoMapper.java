package org.ubiquia.core.flow.service.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.model.dto.AgentCommunicationLanguageDto;
import org.ubiquia.core.flow.model.entity.AgentCommunicationLanguage;

@Service
public class AgentCommunicationLanguageDtoMapper extends GenericDtoMapper<
    AgentCommunicationLanguage,
    AgentCommunicationLanguageDto> {

    @Autowired
    private GraphDtoMapper graphDTOMapper;

    @Override
    public AgentCommunicationLanguageDto map(final AgentCommunicationLanguage from)
        throws JsonProcessingException {

        AgentCommunicationLanguageDto to = null;
        if (Objects.nonNull(from)) {
            to = new AgentCommunicationLanguageDto();
            super.setAEntityFields(from, to);
            to.setJsonSchema(super.objectMapper.readValue(from.getJsonSchema(), Object.class));
            to.setDomain(from.getDomain());
            to.setVersion(from.getVersion());
            to.setGraphs(this.graphDTOMapper.map(from.getGraphs()));
        }
        return to;
    }
}
