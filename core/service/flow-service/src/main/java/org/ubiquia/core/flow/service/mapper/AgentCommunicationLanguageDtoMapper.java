package org.ubiquia.core.flow.service.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.service.mapper.GenericDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguage;
import org.ubiquia.common.model.ubiquia.entity.AgentCommunicationLanguageEntity;

@Service
public class AgentCommunicationLanguageDtoMapper extends GenericDtoMapper<
    AgentCommunicationLanguageEntity,
    AgentCommunicationLanguage> {

    @Autowired
    private GraphDtoMapper graphDTOMapper;

    @Override
    public AgentCommunicationLanguage map(final AgentCommunicationLanguageEntity from)
        throws JsonProcessingException {

        AgentCommunicationLanguage to = null;
        if (Objects.nonNull(from)) {
            to = new AgentCommunicationLanguage();
            super.setAbstractEntityFields(from, to);
            to.setJsonSchema(super.objectMapper.readValue(from.getJsonSchema(), Object.class));
            to.setDomain(from.getDomain());
            to.setVersion(from.getVersion());
            to.setGraphs(this.graphDTOMapper.map(from.getGraphs()));
        }
        return to;
    }
}
